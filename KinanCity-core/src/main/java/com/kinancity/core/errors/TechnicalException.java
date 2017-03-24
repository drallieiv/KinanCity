package com.kinancity.core.errors;

/**
 * Technical error happend, we should retry
 * 
 * @author drallieiv
 *
 */
public class TechnicalException extends AccountCreationException {

	private static final long serialVersionUID = -1173353148853930289L;

	public TechnicalException(Exception e) {
		super(e);
	}

	public TechnicalException(String msg) {
		super(msg);
	}

}
