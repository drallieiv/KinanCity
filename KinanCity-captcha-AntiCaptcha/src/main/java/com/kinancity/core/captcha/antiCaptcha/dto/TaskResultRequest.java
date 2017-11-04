package com.kinancity.core.captcha.antiCaptcha.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskResultRequest {
	private String clientKey;
	private String taskId;
}
