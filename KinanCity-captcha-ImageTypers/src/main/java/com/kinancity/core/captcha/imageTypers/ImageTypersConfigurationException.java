package com.kinancity.core.captcha.imageTypers;

import com.kinancity.core.captcha.CaptchaException;

/**
 * ImageTypers Captcha Errors
 * 
 * @author drallieiv
 *
 */
public class ImageTypersConfigurationException extends CaptchaException {

	private static final long serialVersionUID = -1173353148853930289L;

	public ImageTypersConfigurationException() {
		super();
	}

	public ImageTypersConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImageTypersConfigurationException(String message) {
		super(message);
	}

	public ImageTypersConfigurationException(Throwable cause) {
		super(cause);
	}

}
