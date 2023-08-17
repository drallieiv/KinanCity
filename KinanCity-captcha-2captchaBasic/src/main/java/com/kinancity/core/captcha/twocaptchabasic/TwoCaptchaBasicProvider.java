package com.kinancity.core.captcha.twocaptchabasic;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.impl.BaseCaptchaProvider;
import com.kinancity.core.captcha.impl.BaseConfigurationException;
import com.kinancity.core.captcha.ptc.PtcCaptchaData;
import com.twocaptcha.TwoCaptcha;
import com.twocaptcha.captcha.Captcha;
import com.twocaptcha.captcha.ReCaptcha;
import com.twocaptcha.exceptions.ApiException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class TwoCaptchaBasicProvider extends BaseCaptchaProvider<TwoCaptchaTask> {

    public static final String OPTION_CUSTOM_HOST = "CUSTOM_HOST";

    /**
     * 2 captcha Soft Id;
     */
    private final int KINAN_SOFT_ID = 1816;

    private TwoCaptcha solver;

    public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey) throws CaptchaException {
        return getInstance(queue, apiKey, Optional.empty());
    }

    public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey, Optional<String> altUrl) throws CaptchaException {
        return new TwoCaptchaBasicProvider(queue, apiKey, altUrl);
    }

    public TwoCaptchaBasicProvider(CaptchaQueue queue, String apiKey, Optional<String> altUrl) throws BaseConfigurationException {
        super(queue, apiKey);

        log.debug("Start using TwoCaptchaBasicProvider with api key starting by {}", apiKey.substring(0, 6));

        solver = new TwoCaptcha(apiKey);
        solver.setSoftId(KINAN_SOFT_ID);

        // Custom Host for 2captcha alt
        altUrl.ifPresent(solver::setHost);
        altUrl.ifPresent(url -> log.info("TwoCaptchaBasicProvider with custom host : {}", url));

    }

    @Override
    public double getBalance() throws TechnicalException {
        try {
            return solver.balance();
        } catch (Exception e) {
            throw new TechnicalException("Error getting balance", e);
        }
    }

    @Override
    protected void checkTaskStatusAndProcess(TwoCaptchaTask task) {

        try {
            // Update Captcha Object
            String result = solver.getResult(task.getTaskId());

            if (result != null) {
                this.onTaskSuccess(task);
                this.sendSolutionToQueue(result);
            }
            // Otherwise, we just wait

        } catch (Exception e) {
            log.error("Error getting Captcha Info", e);
            task.setCanBeRetried(false);
            this.onTaskFailed(task);
        }
    }

    @Override
    protected void createNewCaptchaTasks(int nbToRequest) {
        for (int i = 0; i < nbToRequest; i++) {
            if(isHasBalanceLeft()) {
                try {
                    String captchaId  = solver.send(getNewCaptchaTask());
                    this.onTaskCreationSuccess(new TwoCaptchaTask(captchaId));
                } catch (Exception e) {
                    if (e instanceof ApiException && e.getMessage().contains("ERROR_ZERO_BALANCE")) {
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

    private Captcha getNewCaptchaTask() {
        ReCaptcha captcha = new ReCaptcha();
        captcha.setSoftId(KINAN_SOFT_ID);
        captcha.setSiteKey(PtcCaptchaData.GOOGLE_SITE_KEY);
        captcha.setUrl(PtcCaptchaData.PAGE_URL);
        return captcha;
    }
}
