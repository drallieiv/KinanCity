package com.kinancity.core.errors;

/**
 * Something blocked account creation, there is no need to retry
 * 
 * @author drallieiv
 *
 */
public class FatalException extends AccountCreationException {

	private static final long serialVersionUID = -1173353148853930289L;

	public FatalException(Exception e) {
		super(e);
	}

	public FatalException(String msg) {
		super(msg);
	}

}
