package com.kinancity.api.errors.tech;

import java.io.IOException;

import com.kinancity.api.errors.TechnicalException;

/**
 * Exception if the HTTP connection failed.
 * 
 * You mignt want to check your proxies
 * 
 * @author drallieiv
 *
 */
public class HttpConnectionException extends TechnicalException {

	private static final long serialVersionUID = -2772316717114378859L;

	public HttpConnectionException(Exception e) {
		super(e);
	}

	public HttpConnectionException(String msg, IOException e) {
		super(msg, e);
	}

	public HttpConnectionException(String msg) {
		super(msg);
	}

}
