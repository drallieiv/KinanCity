package com.kinancity.core.captcha.impl;


import com.kinancity.core.captcha.CaptchaException;

/**
 * AntiCaptcha Captcha Errors
 * 
 * @author drallieiv
 *
 */
public class BaseConfigurationException extends CaptchaException {

	private static final long serialVersionUID = -1173353148853930289L;

	public BaseConfigurationException() {
		super();
	}

	public BaseConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public BaseConfigurationException(String message) {
		super(message);
	}

	public BaseConfigurationException(Throwable cause) {
		super(cause);
	}

}
