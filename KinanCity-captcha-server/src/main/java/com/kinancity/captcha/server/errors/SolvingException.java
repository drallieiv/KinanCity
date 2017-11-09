package com.kinancity.captcha.server.errors;

public class SolvingException extends Exception {

	private static final long serialVersionUID = 1560853453035324154L;

	public SolvingException() {
		super();
	}

	public SolvingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public SolvingException(String message, Throwable cause) {
		super(message, cause);
	}

	public SolvingException(String message) {
		super(message);
	}

	public SolvingException(Throwable cause) {
		super(cause);
	}

}
