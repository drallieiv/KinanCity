package com.kinancity.core.captcha.antiCaptcha.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateTaskRequest {
	private String clientKey;
	private AbstractTaskDto task;
	private String softId;
	private String languagePool;
}
