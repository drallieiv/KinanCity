package com.pallettown.core.errors;

import java.io.IOException;

/**
 * Error when solving captcha
 * 
 * @author drallieiv
 *
 */
public class CaptchaSolvingException extends AccountCreationException {

	private static final long serialVersionUID = -1173353148853930289L;

	public CaptchaSolvingException(Exception e) {
		super(e);
	}

	public CaptchaSolvingException(String msg) {
		super(msg);
	}

}
