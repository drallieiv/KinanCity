package com.kinancity.core.captcha.deathByCaptcha;


import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.impl.BaseConfigurationException;

/**
 * AntiCaptcha Captcha Errors
 * 
 * @author drallieiv
 *
 */
public class DbsConfigurationException extends BaseConfigurationException {

	private static final long serialVersionUID = -1173353148853930289L;

	public DbsConfigurationException() {
		super();
	}

	public DbsConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public DbsConfigurationException(String message) {
		super(message);
	}

	public DbsConfigurationException(Throwable cause) {
		super(cause);
	}

}
