package com.kinancity.core.captcha.antiCaptcha;


import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AntiCaptchaRequest {

	/**
	 * Id of the ImageTypers request
	 */
	private String captchaId;

	/**
	 * When was it first sent
	 */
	private LocalDateTime sentTime;

	/**
	 * How many times we tried to get it from ImageTypers
	 */
	private int nbPolls;

	public AntiCaptchaRequest(String captchaId) {
		this.captchaId = captchaId;
		this.sentTime = LocalDateTime.now();
	}
}
