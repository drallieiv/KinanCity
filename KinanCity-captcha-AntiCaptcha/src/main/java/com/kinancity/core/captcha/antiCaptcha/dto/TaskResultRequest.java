package com.kinancity.core.captcha.antiCaptcha.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResultRequest {
	private String clientKey;
	private String taskId;
}
