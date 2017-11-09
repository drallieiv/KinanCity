package com.kinancity.core.captcha.antiCaptcha.dto;

import lombok.Data;

@Data
public class CreateTaskResponse {
    private Integer errorId = 0;
    private String errorCode;
    private String errorDescription;
    private Integer taskId;
}
