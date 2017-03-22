package com.kinancity.core.creation;

import com.kinancity.core.errors.AccountCreationException;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PtcCreationResult {

	// True if creation was successfull
	private boolean success;

	// Details
	private String message;
	
	// Error if creation has failed
	private AccountCreationException error;
}
