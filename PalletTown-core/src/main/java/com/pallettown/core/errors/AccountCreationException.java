package com.pallettown.core.errors;

/**
 * Base account creation error
 * @author drallieiv
 *
 */
public class AccountCreationException extends Exception {

	private static final long serialVersionUID = -1173353148853930289L;

	public AccountCreationException() {
		super();
	}

	public AccountCreationException(String message, Throwable cause) {
		super(message, cause);
	}

	public AccountCreationException(String message) {
		super(message);
	}

	public AccountCreationException(Throwable cause) {
		super(cause);
	}

}
