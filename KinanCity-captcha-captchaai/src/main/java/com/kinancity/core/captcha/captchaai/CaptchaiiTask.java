package com.kinancity.core.captcha.captchaai;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CaptchaiiTask {
    private int nbTries = 0;
    private String taskId;
    private LocalDateTime sentTime;

    public CaptchaiiTask(String taskId) {
        this.taskId = taskId;
        this.sentTime = LocalDateTime.now();
    }
}
