package com.kinancity.api.errors;

/**
 * Something blocked account creation, there is no need to retry
 * 
 * @author drallieiv
 *
 */
public class FatalException extends AccountCreationException {

	private static final long serialVersionUID = -1173353148853930289L;

	public FatalException(String msg) {
		super(msg);
	}

	public FatalException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public FatalException(Throwable cause) {
		super(cause);
	}

}
