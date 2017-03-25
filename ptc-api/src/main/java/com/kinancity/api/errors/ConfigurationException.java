package com.kinancity.api.errors;

/**
 * Base account creation error
 * @author drallieiv
 *
 */
public class ConfigurationException extends Exception {

	private static final long serialVersionUID = -1173353148853930289L;

	public ConfigurationException() {
		super();
	}

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConfigurationException(String message) {
		super(message);
	}

	public ConfigurationException(Throwable cause) {
		super(cause);
	}

}
