package com.kinancity.core.captcha.capsolver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.capsolver.dto.request.BalanceRequestDto;
import com.kinancity.core.captcha.capsolver.dto.request.CreateTaskRequestDto;
import com.kinancity.core.captcha.capsolver.dto.request.TaskResultRequestDto;
import com.kinancity.core.captcha.capsolver.dto.response.BalanceResponseDto;
import com.kinancity.core.captcha.capsolver.dto.response.BaseResponseDto;
import com.kinancity.core.captcha.capsolver.dto.response.CreateTaskResponseDto;
import com.kinancity.core.captcha.capsolver.dto.response.GetTaskResulResponseDto;
import lombok.Setter;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CapsolverCaptchaProvider extends CaptchaProvider {

    private static final String BASE_URL = "https://api.capsolver.com";
    private static final String BALANCE_URL = BASE_URL + "/getBalance";
    private static final String NEW_TASK_URL = BASE_URL + "/createTask";
    private static final String GET_TASK_URL = BASE_URL + "/getTaskResult";


    private static final String HTTP_ERROR_MSG = "Could not reach Capsolver servers";
    public static final String ERROR_ZERO_BALANCE = "ERROR_ZERO_BALANCE";

    private final ObjectMapper objectMapper;
    private String apiKey;
    private OkHttpClient captchaClient;
    private CaptchaQueue queue;
    private boolean runFlag = true;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private int maxRequestPerTask = 120;

    private List<CapsolverTask> tasks;

    private boolean hasBalanceLeft = true;

    private boolean addLoggingInterceptor = false;

    @Setter
    private boolean hasUnlimitedSubscription = false;

    /**
     * Wait at least that time (in seconds) before sending first resolve request. (default 5s)
     */
    @Setter
    private int minTimeBeforeFirstResolve = 5;

    /**
     * How often should we call Capsolver (default 5000 ms)
     */
    @Setter
    private int waitBeforeRetry = 5000;

    @Setter
    private int waitForBalance = 5*60*1000;

    public CapsolverCaptchaProvider(CaptchaQueue queue, String apiKey) throws CaptchaException {
        this.queue = queue;
        this.apiKey = apiKey;

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        if (addLoggingInterceptor) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }

        this.captchaClient = builder.build();

        this.tasks = new ArrayList<>();

        this.objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (this.apiKey == null || this.apiKey.isEmpty()) {
            throw new CaptchaException("Missing Capsolver API key");
        }
    }

    public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey) throws CaptchaException {
        return new CapsolverCaptchaProvider(queue, apiKey);
    }

    @Override
    public void run() {
        while (runFlag) {

            if(!hasBalanceLeft) {
                try {
                    if (this.getBalance() > 0) {
                        hasBalanceLeft = true;
                    }
                } catch (TechnicalException | CapsolverConfigurationException e) {
                    logger.error("Error getting balance", e);
                }
            }

            if(hasBalanceLeft) {

                LocalDateTime minDate = LocalDateTime.now().minusSeconds(minTimeBeforeFirstResolve);
                Set<CapsolverTask> captchaiiTasksToResolve = tasks.stream().filter(c -> c.getSentTime().isBefore(minDate)).collect(Collectors.toSet());


                if (captchaiiTasksToResolve.isEmpty()) {
                    // No captcha waiting
                    logger.debug("No captcha check needed, {} in queue", tasks.size());
                } else {
                    // Waiting for captchas
                    logger.debug("Check status of {} captchas", captchaiiTasksToResolve.size());

                    for (CapsolverTask task : captchaiiTasksToResolve) {
                        try {
                            Request taskResultRequest = buildGetTaskResultRequest(task.getTaskId());
                            Response solveResponse = captchaClient.newCall(taskResultRequest).execute();
                            String body = solveResponse.body().string();

                            GetTaskResulResponseDto taskResponse = objectMapper.readValue(body, GetTaskResulResponseDto.class);

                            this.manageTaskResulResponse(task, taskResponse);

                        } catch (CapsolverConfigurationException e) {
                            logger.error("Error while creating GetTaskResult request : {}", e.getMessage());
                        } catch (IOException e) {
                            logger.error("Error while calling captcha provider : {}", e.getMessage());
                        }

                    }
                }

                // Update queue size

                // Number of elements waiting for a captcha
                int nbInQueue = queue.size();

                // Number currently waiting
                int nbWaiting = tasks.size();

                // How many more do we need
                int nbNeeded = Math.min(nbInQueue, getMaxParallelChallenges());

                int nbToRequest = Math.max(0, nbNeeded - nbWaiting);
                if (nbToRequest > 0) {
                    try {
                        // Send new captcha requests
                        Request sendRequest = buildSendCaptchaRequest();
                        for (int i = 0; i < nbToRequest; i++) {
                            try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
                                String body = sendResponse.body().string();

                                CreateTaskResponseDto taskResponse = objectMapper.readValue(body, CreateTaskResponseDto.class);

                                if (taskResponse.isError()) {
                                    if (ERROR_ZERO_BALANCE.equals(taskResponse.getErrorCode())) {
                                        hasBalanceLeft = false;
                                    }
                                    throw generateException(taskResponse);
                                }

                                logger.info("Requested new Captcha, taskid : {}", taskResponse.getTaskId());
                                tasks.add(new CapsolverTask(taskResponse.getTaskId()));

                            } catch (TechnicalException | IOException e) {
                                logger.error("Error while calling captcha provider : {}", e.getMessage());
                            }

                            if (!hasBalanceLeft) {
                                break;
                            }
                        }
                    } catch (CapsolverConfigurationException e) {
                        logger.error("Error while creating new task request : {}", e.getMessage());
                    }
                }
                try {
                    Thread.sleep(waitBeforeRetry);
                } catch (InterruptedException e) {
                    logger.error("Interrupted");
                }
            } else {
                if (waitForBalance > 0) {
                    logger.warn("Will wait for {}s before checking balance again", waitForBalance);
                    try {
                        Thread.sleep(waitForBalance);
                    } catch (InterruptedException e) {
                        logger.error("Interrupted");
                    }
                } else {
                    logger.warn("Stopping captcha provider");
                    this.runFlag = false;
                }
            }
        }
    }

    private void manageTaskResulResponse(CapsolverTask task, GetTaskResulResponseDto taskResponse) {
        if ("Idle".equals(taskResponse.getStatus())) {
            logger.debug("Task {} Idle, will retry later", task.getTaskId());
            task.setNbTries(task.getNbTries() + 1);
        } else if("processing".equals(taskResponse.getStatus())){
            logger.debug("Task {} Processing, will retry later", task.getTaskId());
            if(task.getNbTries() + 1 > maxRequestPerTask) {
                logger.warn("Too many retries on task, forfeit");
                tasks.remove(task);
            } if (getMaxWait() > 0 && LocalDateTime.now().isAfter(task.getSentTime().plusSeconds(getMaxWait()))) {
                logger.warn("Retries too too long, forfeit");
                tasks.remove(task);
            } else {
                task.setNbTries(task.getNbTries() + 1);
            }
        } else if("failed".equals(taskResponse.getStatus())) {
            logger.error("Task {} failed : {}", task.getTaskId(), taskResponse.getErrorDescription());
            tasks.remove(task);
        } else if("ready".equals(taskResponse.getStatus())) {
            String captchaSolution = taskResponse.getSolution().getGRecaptchaResponse();
            logger.debug("Task {} solved : {}", task.getTaskId(), captchaSolution);
            queue.addCaptcha(captchaSolution);
            tasks.remove(task);
        } else if(taskResponse.getStatus() == null && StringUtils.isNotEmpty(taskResponse.getErrorCode())) {
            logger.error("Error for task {} : {}, {}", task.getTaskId(), taskResponse.getErrorCode(), taskResponse.getErrorDescription());
            tasks.remove(task);
        } else {
            logger.error("Unknown status : {}", taskResponse.getStatus());
        }
    }

    /**
     * Get Current Balance. Should be called at least once before use to check for valid key.
     *
     * @return current balance in USD
     * @throws TechnicalException
     * @throws CapsolverConfigurationException
     */
    public double getBalance() throws TechnicalException, CapsolverConfigurationException {
        Request sendRequest = buildBalanceCheckequest();
        try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
            String body = sendResponse.body().string();
            BalanceResponseDto balanceResponse = objectMapper.readValue(body, BalanceResponseDto.class);

            if (balanceResponse.isError()) {
                throw generateException(balanceResponse);
            }

            return balanceResponse.getBalance();
        } catch (IOException e) {
            throw new TechnicalException(HTTP_ERROR_MSG, e);
        }
    }

    private TechnicalException generateException(BaseResponseDto responseDto) {
        return new TechnicalException(
                (new StringBuilder())
                        .append("Capsolver Error, ")
                        .append(responseDto.getErrorCode())
                        .append(" : ")
                        .append(responseDto.getErrorDescription())
                        .toString());
    }


    private Request buildGetTaskResultRequest(String taskId) throws CapsolverConfigurationException {
        try {
            HttpUrl url = HttpUrl.parse(GET_TASK_URL).newBuilder()
                    .build();

            String jsonBody = objectMapper.writeValueAsString(new TaskResultRequestDto(apiKey, taskId));

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jsonBody);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            return request;
        } catch (JsonProcessingException e) {
            throw new CapsolverConfigurationException("Failed creating new taskResult request", e);
        }
    }


    private Request buildSendCaptchaRequest() throws CapsolverConfigurationException {
        try {
            HttpUrl url = HttpUrl.parse(NEW_TASK_URL).newBuilder()
                    .build();

            String jsonBody = objectMapper.writeValueAsString(new CreateTaskRequestDto(apiKey));

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jsonBody);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            return request;
        } catch (JsonProcessingException e) {
            throw new CapsolverConfigurationException("Failed creating new task request", e);
        }
    }


    /**
     * Create check balance request.
     *
     * @return Request to sent for balance check
     */
    private Request buildBalanceCheckequest() throws CapsolverConfigurationException {
        try {
            HttpUrl url = HttpUrl.parse(BALANCE_URL).newBuilder()
                    .build();

            String jsonBody = objectMapper.writeValueAsString(new BalanceRequestDto(apiKey));

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json"), jsonBody);

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            return request;
        } catch (JsonProcessingException e) {
            throw new CapsolverConfigurationException("Failed creating balance request", e);
        }
    }

}
