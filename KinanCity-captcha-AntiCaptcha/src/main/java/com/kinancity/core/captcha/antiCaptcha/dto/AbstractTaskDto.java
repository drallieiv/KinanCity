package com.kinancity.core.captcha.antiCaptcha.dto;

import lombok.Data;

@Data
public abstract class AbstractTaskDto {
	private String type;
	private String websiteURL;
	private String websiteKey;
}
