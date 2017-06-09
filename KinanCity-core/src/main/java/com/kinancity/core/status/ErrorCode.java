package com.kinancity.core.status;

import lombok.Getter;

@Getter
public enum ErrorCode {

	/**
	 * Username or password refused
	 */
	BAD_USERNAME_OR_PASSWORD("BAD_USERNAME_OR_PASSWORD", "Account is not valid : bad password or username taken"),
	
	/**
	 * Duplicate
	 */
	ACCOUNT_DUPLICATE("ACCOUNT_DUPLICATE", "Account with this username already exists"),
	
	/**
	 * Duplicate
	 */
	EMAIL_DUPLICATE("EMAIL_DUPLICATE", "Email duplicate or domain blocked"),

	/**
	 * Captcha solving failed
	 */
	CAPTCHA_SOLVING("CAPTCHA_SOLVING", "Error solving captcha"),
	
	/**
	 * Technical Error
	 */
	NETWORK_ERROR("NETWORK_ERROR", "Network Error"),
	
	/**
	 * Technical Error
	 */
	TECH_ERROR("TECH_ERROR", "Technical Error"),
	
	/**
	 * FATAL_ERROR
	 */
	FATAL_ERROR("FATAL_ERROR", "Fatal Error");

	private String code;

	private String message;

	private ErrorCode(String code, String message) {
		this.message = message;
		this.code = code;
	}

}
