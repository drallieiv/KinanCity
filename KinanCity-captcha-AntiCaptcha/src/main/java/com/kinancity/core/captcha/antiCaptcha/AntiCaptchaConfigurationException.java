package com.kinancity.core.captcha.antiCaptcha;


import com.kinancity.core.captcha.CaptchaException;

/**
 * AntiCaptcha Captcha Errors
 * 
 * @author drallieiv
 *
 */
public class AntiCaptchaConfigurationException extends CaptchaException {

	private static final long serialVersionUID = -1173353148853930289L;

	public AntiCaptchaConfigurationException() {
		super();
	}

	public AntiCaptchaConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AntiCaptchaConfigurationException(String message) {
		super(message);
	}

	public AntiCaptchaConfigurationException(Throwable cause) {
		super(cause);
	}

}
