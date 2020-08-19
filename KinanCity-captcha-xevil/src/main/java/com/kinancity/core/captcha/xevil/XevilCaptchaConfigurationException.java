package com.kinancity.core.captcha.xevil;

import com.kinancity.core.captcha.CaptchaException;

/**
 * Xevil Captcha Errors
 * 
 * @author drallieiv
 *
 */
public class XevilCaptchaConfigurationException extends CaptchaException {

	private static final long serialVersionUID = -1173353148853930289L;

	public XevilCaptchaConfigurationException() {
		super();
	}

	public XevilCaptchaConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public XevilCaptchaConfigurationException(String message) {
		super(message);
	}

	public XevilCaptchaConfigurationException(Throwable cause) {
		super(cause);
	}

}
