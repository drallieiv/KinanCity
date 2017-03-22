package com.pallettown.core.errors;

/**
 * Exception when the number of account per minute is reached for that IP
 * 
 * @author drallieiv
 *
 */
public class AccountRateLimitExceededException extends AccountCreationException {

	private static final long serialVersionUID = -3151934623808680405L;

	public AccountRateLimitExceededException() {
		super("Rate Limit Exceeded");
	}
	
	public AccountRateLimitExceededException(String msg) {
		super(msg);
	}


}
