package com.kinancity.core.captcha.capsolver.dto.request;

import lombok.Data;

@Data
public class TaskResultRequestDto extends BaseRequestDto {
    private String taskId;
    public TaskResultRequestDto(String clientKey, String taskId) {
        super(clientKey);
        this.taskId = taskId;
    }
}
