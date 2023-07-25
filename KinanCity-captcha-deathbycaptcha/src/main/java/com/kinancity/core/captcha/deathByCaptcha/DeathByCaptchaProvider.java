package com.kinancity.core.captcha.deathByCaptcha;

import com.DeathByCaptcha.Exception;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.DeathByCaptcha.*;
import com.kinancity.core.captcha.impl.BaseCaptchaProvider;
import com.kinancity.core.captcha.impl.BaseConfigurationException;
import com.kinancity.core.captcha.ptc.PtcCaptchaData;
import lombok.extern.slf4j.Slf4j;
import uk.org.lidalia.sysoutslf4j.context.LogLevel;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.IOException;

@Slf4j
public class DeathByCaptchaProvider extends BaseCaptchaProvider<DeathByCaptchaTask> {

    private final Client captchaClient;

    private final int PTC_CAPTCHA_TYPE = 4;


    public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey) throws CaptchaException {
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J(LogLevel.TRACE, LogLevel.WARN);
        return new DeathByCaptchaProvider(queue, apiKey);
    }

    public DeathByCaptchaProvider(CaptchaQueue queue, String apiKey) throws BaseConfigurationException {
        super(queue, apiKey);

        if (apiKey == null) {
            throw new DbsConfigurationException("Missing Captcha Provider Access Token");
        }

        if (apiKey.contains(":")) {
            String[] keyParts = apiKey.split(":");
            log.debug("Create client with Login:Pass");
            this.captchaClient = new SocketClient(keyParts[0], keyParts[1]);
        } else {
            log.debug("Create client with api key");
            this.captchaClient = new SocketClient(apiKey);
        }

        try {
            User userInfo = captchaClient.getUser();
            log.debug("User info : account id {}, current balance {}", userInfo.id, userInfo.balance);
            if (userInfo.isBanned) {
                throw new DbsConfigurationException("Error, your DeathByCaptcha account is banned");
            }
        } catch (IOException | Exception e) {
            throw new DbsConfigurationException("Error, failed to login to DeathByCaptcha to get Account info", e);
        }

    }

    @Override
    public double getBalance() throws TechnicalException {
        try {
            return captchaClient.getBalance();
        } catch (IOException | Exception e) {
            throw new TechnicalException("Error getting balance", e);
        }
    }

    @Override
    protected void checkTaskStatusAndProcess(DeathByCaptchaTask task) {
        try {
            // Update Captcha Object
            Captcha captcha = captchaClient.getCaptcha(task.getCaptcha());
            task.setCaptcha(captcha);

            if (captcha.isSolved()) {
                if (captcha.isCorrect()) {
                    this.onTaskSuccess(task);
                    this.sendSolutionToQueue(captcha.text);
                } else {
                    log.error("Incorrect captcha solve : {}", captcha.text);
                    task.setCanBeRetried(false);
                    this.onTaskFailed(task);
                }
            }

            // Otherwise, we just wait

        } catch (IOException | Exception e) {
            log.error("Error getting Captcha Info", e);
            this.onTaskFailed(task);
        }
    }

    @Override
    protected void createNewCaptchaTasks(int nbToRequest) {
        for (int i = 0; i < nbToRequest; i++) {
            if(isHasBalanceLeft()) {
                try {
                    Captcha captcha = captchaClient.upload(PTC_CAPTCHA_TYPE, PtcCaptchaData.GOOGLE_SITE_KEY, PtcCaptchaData.PAGE_URL);
                    this.onTaskCreationSuccess(new DeathByCaptchaTask(captcha));
                } catch (IOException | Exception e) {
                    if (e instanceof AccessDeniedException && e.getMessage().contains("balance is too low")) {
                        this.onZeroBalanceError();
                    } else {
                        log.error("Failed to create Captcha Task", e);
                    }
                }
            } else {
                log.warn("Zero Balance reached, skip remaining captcha tasks creations");
            }
        }
    }
}
