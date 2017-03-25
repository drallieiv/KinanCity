package com.kinancity.api.errors;

/**
 * Technical error happend, but you can retry
 * 
 * @author drallieiv
 *
 */
public class TechnicalException extends AccountCreationException {

	private static final long serialVersionUID = -1173353148853930289L;

	public TechnicalException(String msg) {
		super(msg);
	}

	public TechnicalException(String msg, Throwable cause) {
		super(msg, cause);
	}

	public TechnicalException(Throwable cause) {
		super(cause);
	}

}
