package com.kinancity.core.captcha.twoCaptcha;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class TwoCaptchaChallenge {

	/**
	 * Id of the 2Captcha request
	 */
	private String captchaId;

	/**
	 * When was it first sent
	 */
	private LocalDateTime sentTime;

	/**
	 * How many times we tried to get it from 2Captcha
	 */
	private int nbPolls;

	public TwoCaptchaChallenge(String captchaId) {
		this.captchaId = captchaId;
		this.sentTime = LocalDateTime.now();
	}
}
