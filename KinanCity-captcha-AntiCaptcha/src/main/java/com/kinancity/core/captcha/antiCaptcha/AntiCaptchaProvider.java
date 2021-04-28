package com.kinancity.core.captcha.antiCaptcha;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.antiCaptcha.dto.BalanceRequest;
import com.kinancity.core.captcha.antiCaptcha.dto.BalanceResponse;
import com.kinancity.core.captcha.antiCaptcha.dto.CreateTaskRequest;
import com.kinancity.core.captcha.antiCaptcha.dto.CreateTaskResponse;
import com.kinancity.core.captcha.antiCaptcha.dto.PtcCaptchaTask;
import com.kinancity.core.captcha.antiCaptcha.dto.TaskResultRequest;
import com.kinancity.core.captcha.antiCaptcha.dto.TaskResultResponse;
import com.kinancity.core.captcha.antiCaptcha.errors.ErrorCode;

import lombok.Getter;
import lombok.Setter;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AntiCaptchaProvider extends CaptchaProvider {

	private CaptchaQueue queue;

	/**
	 * List of AntiCaptcha Challenges sent
	 */
	private List<AntiCaptchaRequest> challenges = new ArrayList<>();

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Captcha API key / Client Key
	 */
	private String apiKey = null;

	/**
	 * AntiCaptcha Id;
	 */
	private String softId = "864";

	/**
	 * Wait at least that time (in seconds) before sending first resolve request. (default 5s)
	 */
	@Setter
	private int minTimeBeforeFirstResolve = 5;

	/**
	 * How often should we call the provider (default 5000 ms)
	 */
	@Setter
	private int waitBeforeRetry = 5000;

	private boolean runFlag = true;

	private boolean stopOnInsufficientBalance = true;

	public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

	public String RECAPTCHA_SUBMIT_ENDPOINT = "https://api.anti-captcha.com/createTask";
	public String RECAPTCHA_RETRIEVE_ENDPOINT = "https://api.anti-captcha.com/getTaskResult";
	public String BALANCE_ENDPOINT = "https://api.anti-captcha.com/getBalance ";

	@Setter
	public String captchaSubmitUrl = RECAPTCHA_SUBMIT_ENDPOINT;
	@Setter
	public String captchaRetrieveUrl = RECAPTCHA_RETRIEVE_ENDPOINT;
	@Setter
	public String captchaBalanceUrl = BALANCE_ENDPOINT;

	private OkHttpClient captchaClient = new OkHttpClient();

	private ObjectMapper mapper = new ObjectMapper();

	// Count the number of captcha sent
	@Getter
	private int nbSent = 0;

	public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey) throws CaptchaException {
		return new AntiCaptchaProvider(queue, apiKey);
	}

	public AntiCaptchaProvider(CaptchaQueue queue, String apiKey) throws AntiCaptchaConfigurationException {
		this.queue = queue;
		this.apiKey = apiKey;
		this.captchaClient = new OkHttpClient();

		if (this.apiKey == null || this.apiKey.isEmpty()) {
			throw new AntiCaptchaConfigurationException("Missing Captcha Provider Access Token");
		}
	}

	@Override
	public void run() {

		Request sendRequest;
		try {
			sendRequest = buildSendCaptchaRequest();
		} catch (JsonProcessingException e2) {
			throw new RuntimeException("Error generating AntiCaptcha Provider requests", e2);
		}

		while (runFlag) {

			LocalDateTime minDate = LocalDateTime.now().minusSeconds(minTimeBeforeFirstResolve);
			Set<AntiCaptchaRequest> challengesToResolve = challenges.stream().filter(c -> c.getSentTime().isBefore(minDate)).collect(Collectors.toSet());

			if (challengesToResolve.isEmpty()) {
				// No captcha waiting
				logger.debug("No captcha check needed, {} in queue", challenges.size());
			} else {
				// Currently waiting for captcha
				logger.debug("Check status of {} captchas", challengesToResolve.size());

				for (AntiCaptchaRequest challenge : challengesToResolve) {
					String captchaId = challenge.getCaptchaId();
					try {
						Request receiveRequest = buildReceiveCaptchaRequest(captchaId);

						try (Response sendResponse = captchaClient.newCall(receiveRequest).execute()) {
							String body = sendResponse.body().string();

							TaskResultResponse response = mapper.readValue(body, TaskResultResponse.class);

							if (response.getErrorId() > 0) {

								try {
									ErrorCode errorCode = ErrorCode.valueOf(response.getErrorCode());
									logger.error("Captcha Provider Error {}", response.getErrorCode());

									if (ErrorCode.ERROR_ZERO_BALANCE.equals(errorCode)) {
										if (stopOnInsufficientBalance) {
											logger.error("STOP");
											this.runFlag = false;
											break;
										} else {
											logger.error("WAIT");
											try {
												Thread.sleep(30000);
											} catch (InterruptedException e1) {
												// Interrupted
											}
										}
									} else if (ErrorCode.ERROR_NO_SLOT_AVAILABLE.equals(errorCode)) {
										// No slots, wait for a bit
										logger.warn("NO_SLOT_AVAILABLE, Wait for a bit");
										try {
											Thread.sleep(30000);
										} catch (InterruptedException e1) {
											// Interrupted
										}
									} else if (ErrorCode.ERROR_NO_SUCH_CAPCHA_ID.equals(errorCode)) {
										logger.warn("ERROR_NO_SUCH_CAPCHA_ID : {}", captchaId);
										challenges.remove(challenge);
									} else if (ErrorCode.ERROR_CAPTCHA_UNSOLVABLE.equals(errorCode)) {
										logger.warn("ERROR_CAPTCHA_UNSOLVABLE : {}", captchaId);
										challenges.remove(challenge);
									} else {
										logger.warn("OTHER ERROR with code {}", errorCode);
										challenges.remove(challenge);
									}

								} catch (IllegalArgumentException e) {
									logger.error("Unknown Captcha Provider Error Code : {}", response.getErrorCode());
									challenges.remove(challenge);
								}
							} else {
								// NO ERROR
								if (!response.getStatus().equals(TaskResultResponse.READY)) {
									// Captcha not ready, should wait
									if (getMaxWait() > 0 && LocalDateTime.now().isAfter(challenge.getSentTime().plusSeconds(getMaxWait()))) {
										logger.error("This captcha has been waiting too long, drop it. Increase `maxWait` if that happens too often");
										challenges.remove(challenge);
									} else {
										// Keep the challenge in queue
										challenge.setNbPolls(challenge.getNbPolls() + 1);
									}
								} else {
									// Captcha is ready use it
									String captcha = response.getSolution().getGRecaptchaResponse();
									logger.info("Captcha response given in {}s", ChronoUnit.SECONDS.between(challenge.getSentTime(), LocalDateTime.now()));
									queue.addCaptcha(captcha);
									challenges.remove(challenge);
								}
							}
						} catch (IOException e) {
							logger.error("Captcha Provider Error : " + e.getMessage(), e);
							logger.debug("Captcha Provider Error details", e);
							challenges.remove(challenge);
						}
					} catch (JsonProcessingException e) {
						logger.error("Captcha Provider Error : " + e.getMessage(), e);
					}
				}
			}

			// Update queue size

			// Number of elements waiting for a captcha
			int nbInQueue = queue.size();

			// Number currently waiting at 2captcha
			int nbWaiting = challenges.size();

			// How many more do we need
			int nbNeeded = Math.min(nbInQueue, getMaxParallelChallenges());

			int nbToRequest = Math.max(0, nbNeeded - nbWaiting);
			if (nbToRequest > 0) {
				// Send new captcha requests
				
				logger.info("{} new Captcha required", nbToRequest);

				for (int i = 0; i < nbToRequest; i++) {
					try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {

						if (sendResponse.isSuccessful()) {

							String body = sendResponse.body().string();
							CreateTaskResponse response = mapper.readValue(body, CreateTaskResponse.class);

							if (response.getErrorId() == 0) {
								// Okay
								String captchaId = response.getTaskId().toString();
								logger.info("Requested new Captcha, id : {}", captchaId);
								challenges.add(new AntiCaptchaRequest(captchaId));
							} else {
								// Error
								try {
									ErrorCode errorCode = ErrorCode.valueOf(response.getErrorCode());
									logger.error("Captcha Provider Error {}", response.getErrorCode());

									if (errorCode == ErrorCode.ERROR_ZERO_BALANCE) {
										if (stopOnInsufficientBalance) {
											logger.error("STOP");
											this.runFlag = false;
											break;
										} else {
											logger.error("WAIT");
											try {
												Thread.sleep(30000);
											} catch (InterruptedException e1) {
												// Interrupted
											}
										}
									}

								} catch (IllegalArgumentException e) {
									logger.error("Unknown Captcha Provider Error Code : {}", response.getErrorCode());
								}
							}
						} else{
							logger.error("Captcha Provider Error HTTP {}", sendResponse.code());
						}
					} catch (IOException e2) {
						logger.error("Unknown Captcha Provider Error : {}", e2.getMessage(), e2);
					}
				}
			}

			try {
				Thread.sleep(waitBeforeRetry);
			} catch (InterruptedException e) {
				logger.error("Interrupted");
			}
		}

	}

	/**
	 * Get Current Balance. Should be called at least once before use to check for valid key.
	 * 
	 * @return current balance in USD
	 * @throws TechnicalException
	 */
	public double getBalance() throws AntiCaptchaConfigurationException {
		try {
			Request sendRequest = buildBalanceCheckequestGet();
			Response sendResponse = captchaClient.newCall(sendRequest).execute();
			BalanceResponse balance = mapper.readValue(sendResponse.body().string(), BalanceResponse.class);

			if (balance.getErrorId() > 0) {
				String errMsg = String.format("Balance Error : %s - %s", balance.getErrorCode(), balance.getErrorDescription());
				throw new AntiCaptchaConfigurationException(errMsg);
			}

			return balance.getBalance();
		} catch (Exception e) {
			throw new AntiCaptchaConfigurationException("Error getting account balance", e);
		}
	}

	/**
	 * Send Captcha Request
	 * 
	 * @return Request
	 * @throws JsonProcessingException
	 */
	private Request buildSendCaptchaRequest() throws JsonProcessingException {

		PtcCaptchaTask task = new PtcCaptchaTask();
		CreateTaskRequest taskRequest = new CreateTaskRequest(apiKey, task, softId, "en");

		HttpUrl url = HttpUrl.parse(captchaSubmitUrl).newBuilder().build();
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(taskRequest));

		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		return request;
	}

	/**
	 * Receive Captcha Request
	 * 
	 * @param catpchaId
	 * @return
	 * @throws JsonProcessingException
	 */
	private Request buildReceiveCaptchaRequest(String catpchaId) throws JsonProcessingException {
		TaskResultRequest resultRequest = new TaskResultRequest(apiKey, catpchaId);
		HttpUrl url = HttpUrl.parse(captchaRetrieveUrl).newBuilder().build();
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(resultRequest));

		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		return request;
	}

	/**
	 * Check Balance
	 * 
	 * @param catpchaId
	 * @return Request
	 * @throws JsonProcessingException
	 */
	private Request buildBalanceCheckequestGet() throws JsonProcessingException {
		HttpUrl url = HttpUrl.parse(captchaBalanceUrl).newBuilder().build();
		BalanceRequest balanceRequest = new BalanceRequest(apiKey);
		RequestBody body = RequestBody.create(MediaType.parse("application/json"), mapper.writeValueAsString(balanceRequest));

		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		return request;
	}
}
