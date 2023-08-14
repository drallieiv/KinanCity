package com.kinancity.captcha.captchaai;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;

import lombok.Data;

@Data
public class CaptchaAiChallenge {

    /**
     * Id of the CaptchaAi request
     */
    private String captchaId;

    /**
     * When was it first sent
     */
    private LocalDateTime sentTime;

    /**
     * How many times we tried to get it from CaptchaAi
     */
    private int nbPolls;

    public CaptchaAiChallenge(String captchaId) {
        this.captchaId = captchaId;
        this.sentTime = LocalDateTime.now();
    }
}
