package com.kinancity.core.captcha.captchaai.dto.request;

import lombok.Data;

@Data
public class TaskResultRequestDto extends BaseRequestDto {
    private String taskId;
    public TaskResultRequestDto(String clientKey, String taskId) {
        super(clientKey);
        this.taskId = taskId;
    }
}
