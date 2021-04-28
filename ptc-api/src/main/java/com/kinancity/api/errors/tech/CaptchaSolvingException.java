package com.kinancity.api.errors.tech;

import com.kinancity.api.errors.TechnicalException;

/**
 * Error when solving captcha
 * 
 * @author drallieiv
 *
 */
public class CaptchaSolvingException extends TechnicalException {

	private static final long serialVersionUID = -1173353148853930289L;

	public CaptchaSolvingException(Exception e) {
		super(e);
	}

	public CaptchaSolvingException(String msg) {
		super(msg);
	}

}