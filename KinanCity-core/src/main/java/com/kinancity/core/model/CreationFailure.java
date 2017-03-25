package com.kinancity.core.model;

import java.util.HashMap;
import java.util.Map;

import com.kinancity.core.status.ErrorCode;

import lombok.Getter;

/**
 * Detail of why the creation as failed
 * 
 * @author drallieiv
 *
 */
@Getter
public class CreationFailure {

	private String errorCode;

	private String errorMessage;
	
	private Exception exception;
	

	public CreationFailure(ErrorCode error) {
		this(error.getCode(), error.getMessage(), null);
	}

	public CreationFailure(ErrorCode error, String message) {
		this(error.getCode(), message, null);
	}

	public CreationFailure(ErrorCode error, Exception exception) {
		this(error.getCode(), error.getMessage(), exception);
	}

	public CreationFailure(ErrorCode error, String message, Exception exception) {
		this(error.getCode(), message, exception);
	}

	public CreationFailure(String errorCode, String message, Exception exception) {
		this.errorCode = errorCode;
		this.errorMessage = message;
		this.exception = exception;
	}

}
