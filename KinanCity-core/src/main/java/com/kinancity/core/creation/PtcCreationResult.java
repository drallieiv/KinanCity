package com.kinancity.core.creation;

import com.kinancity.core.errors.AccountCreationException;

import lombok.Data;

@Data
public class PtcCreationResult {

	// True if creation was successfull
	private boolean success;

	// Details
	private String message;

	// Error if creation has failed
	private AccountCreationException error;

	// True if the creation has been retried
	private boolean rescheduled;

	public PtcCreationResult(boolean success, String message, AccountCreationException error, boolean rescheduled) {
		super();
		this.success = success;
		this.message = message;
		this.error = error;
		this.rescheduled = rescheduled;
	}

	public PtcCreationResult(boolean success, String message, AccountCreationException error) {
		this(success, message, error, false);
	}

}
