package com.kinancity.core.captcha.impl;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Base pending task to have captcha solved.
 */
@Data
@ToString
public class BaseSolvingTask {

    private int nbTries = 0;

    @Setter
    @Getter
    private boolean canBeRetried = true;

    private String taskId;

    private LocalDateTime sentTime;

    public BaseSolvingTask(String taskId) {
        this.taskId = taskId;
        this.sentTime = LocalDateTime.now();
    }

    public void incrementNbTries() {
        nbTries++;
    }
}
