package com.kinancity.api.errors;

/**
 * Base account creation error
 * @author drallieiv
 *
 */
public class TwoCaptchaConfigurationException extends Exception {

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
