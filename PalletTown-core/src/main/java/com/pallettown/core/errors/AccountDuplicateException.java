package com.pallettown.core.errors;

/**
 * Exception when the server respond that the account already exists
 * 
 * @author drallieiv
 *
 */
public class AccountDuplicateException extends AccountCreationException {

	private static final long serialVersionUID = -3151934623808680405L;

	public AccountDuplicateException() {
		super("Account Already Exists");
	}

	
	public AccountDuplicateException(String msg) {
		super(msg);
	}
	
}

