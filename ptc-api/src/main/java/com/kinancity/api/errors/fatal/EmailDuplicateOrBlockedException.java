package com.kinancity.api.errors.fatal;

import com.kinancity.api.errors.FatalException;

/**
 * Exception when the server respond that the account already exists
 * 
 * @author drallieiv
 *
 */
public class EmailDuplicateOrBlockedException extends FatalException {

	private static final long serialVersionUID = -3151934623808680405L;

	public EmailDuplicateOrBlockedException() {
		super("Account Already Exists");
	}

	
	public EmailDuplicateOrBlockedException(String msg) {
		super(msg);
	}
	
}

