package com.kinancity.core.captcha.xevil;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class XevilChallenge {

	/**
	 * Id of the 2Captcha request
	 */
	private String captchaId;

	/**
	 * When was it first sent
	 */
	private LocalDateTime sentTime;

	/**
	 * How many times we tried to get it from Xevil
	 */
	private int nbPolls;

	public XevilChallenge(String captchaId) {
		this.captchaId = captchaId;
		this.sentTime = LocalDateTime.now();
	}
}
