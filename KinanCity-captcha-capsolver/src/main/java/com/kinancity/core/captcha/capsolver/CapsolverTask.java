package com.kinancity.core.captcha.capsolver;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CapsolverTask {
    private int nbTries = 0;
    private String taskId;
    private String appId = "F651F862-8811-421D-9172-80A95BDFB1D3";
    private LocalDateTime sentTime;

    public CapsolverTask(String taskId) {
        this.taskId = taskId;
        this.sentTime = LocalDateTime.now();
    }
}
