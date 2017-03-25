package com.kinancity.api.errors.tech;

import com.kinancity.api.errors.TechnicalException;

/**
 * Exception when the number of account per minute is reached for that IP
 * 
 * @author drallieiv
 *
 */
public class AccountRateLimitExceededException extends TechnicalException {

	private static final long serialVersionUID = -3151934623808680405L;

	public AccountRateLimitExceededException() {
		super("Rate Limit Exceeded");
	}
	
	public AccountRateLimitExceededException(String msg) {
		super(msg);
	}


}
