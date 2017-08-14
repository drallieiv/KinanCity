package com.kinancity.core.captcha.twoCaptcha;

import com.kinancity.core.captcha.CaptchaException;

/**
 * ImageTypers Captcha Errors
 * 
 * @author drallieiv
 *
 */
public class TwoCaptchaConfigurationException extends CaptchaException {

	private static final long serialVersionUID = -1173353148853930289L;

	public TwoCaptchaConfigurationException() {
		super();
	}

	public TwoCaptchaConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public TwoCaptchaConfigurationException(String message) {
		super(message);
	}

	public TwoCaptchaConfigurationException(Throwable cause) {
		super(cause);
	}

}
