package com.kinancity.core.captcha.impl;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseCaptchaProvider<T extends BaseSolvingTask> extends CaptchaProvider {

    private CaptchaQueue queue;
    private String apiKey;
    
    @Setter
    private boolean addLoggingInterceptor = false;

    @Setter
    private boolean hasUnlimitedSubscription = false;

    /**
     * Wait at least that time (in seconds) before sending first resolve request. (default 5s)
     */
    @Setter
    private int minTimeBeforeFirstResolve = 5;

    /**
     * How often should we call the API (default 5000 ms)
     */
    @Setter
    private int waitBeforeRetry = 5000;

    /**
     * How long do we freeze waiting for balance ?
     * Set to 0 to have it stop.
     */
    @Setter
    private int waitForBalance = 5*60*1000;

    /**
     * Do we handle retry for call of cancel the task ?
     */
    @Setter
    private int maxFailureRetry = 0;

    private List<T> tasks;

    private boolean runFlag = true;

    @Getter
    private boolean hasBalanceLeft = true;

    public BaseCaptchaProvider(CaptchaQueue queue, String apiKey) throws BaseConfigurationException {
        this.queue = queue;
        this.apiKey = apiKey;
        this.tasks = new ArrayList<>();
    }

    @Override
    public void run() {

        while (runFlag) {

            if(!hasBalanceLeft) {
                try {
                    if (this.getBalance() > 0) {
                        hasBalanceLeft = true;
                    }
                } catch (TechnicalException | CaptchaException e) {
                    log.error("Error getting balance", e);
                }
            }

            if(hasBalanceLeft) {

                LocalDateTime minDate = LocalDateTime.now().minusSeconds(minTimeBeforeFirstResolve);
                Set<T> tasksToResolve = tasks.stream().filter(c -> c.getSentTime().isBefore(minDate)).collect(Collectors.toSet());

                if (tasksToResolve.isEmpty()) {
                    // No captcha waiting
                    log.debug("No captcha check needed, {} in queue", tasks.size());
                } else {
                    // Waiting for captchas
                    log.debug("Check status of {} captchas", tasksToResolve.size());

                    for (T task : tasksToResolve) {
                        this.checkTaskStatusAndProcess(task);
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
                    this.createNewCaptchaTasks(nbToRequest);
                }
                try {
                    Thread.sleep(waitBeforeRetry);
                } catch (InterruptedException e) {
                    log.error("Interrupted");
                }
            } else {
                if (waitForBalance > 0) {
                    log.warn("Will wait for {}s before checking balance again", waitForBalance);
                    try {
                        Thread.sleep(waitForBalance);
                    } catch (InterruptedException e) {
                        log.error("Interrupted");
                    }
                } else {
                    log.warn("Stopping captcha provider");
                    this.runFlag = false;
                }
            }
        }
    }

    protected abstract void checkTaskStatusAndProcess(T task);

    protected abstract void createNewCaptchaTasks(int nbToRequest);

    protected void onZeroBalanceError() {
        log.warn("Balance has reached 0");
        this.hasBalanceLeft = false;
    }

    protected void onTaskCreationSuccess(T task) {
        log.debug("Captcha task added to checking list with id {}", task.getTaskId());
        tasks.add(task);
    }


    protected void onTaskFailed(T task) {
        if (task.isCanBeRetried() && task.getNbTries() <= maxFailureRetry) {
            log.warn("Task failed but will be retried");
        } else {
            log.warn("Task failed and will be removed");
            tasks.remove(task);
        }
    }

    protected void onTaskSuccess(T task) {
         tasks.remove(task);
    }

    protected void sendSolutionToQueue(String captchaSolution) {
        queue.addCaptcha(captchaSolution);
    }
}
