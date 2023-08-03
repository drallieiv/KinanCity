package com.kinancity.core.captcha.capsolver;

import com.kinancity.core.captcha.CaptchaException;

/**
 * Capsolver Captcha Errors
 * 
 * @author drallieiv
 *
 */
public class CapsolverConfigurationException extends CaptchaException {

	private static final long serialVersionUID = -1173353148853930289L;

	public CapsolverConfigurationException() {
		super();
	}

	public CapsolverConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public CapsolverConfigurationException(String message) {
		super(message);
	}

	public CapsolverConfigurationException(Throwable cause) {
		super(cause);
	}

}
