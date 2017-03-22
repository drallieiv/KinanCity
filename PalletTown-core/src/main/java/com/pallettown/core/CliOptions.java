package com.pallettown.core;

import org.apache.commons.cli.Option;

import lombok.Getter;

@Getter
public enum CliOptions {

	CK("ck", "captchaKey", true, "2 captcha API Key"),

	SINGLE_EMAIL("m", "mail", true, "account email address"),
	SINGLE_USERNAME("u", "username", true, "account username/login"),
	SINGLE_PASSWORD("p", "password", true, "account password"),
	
	MULTIPLE_ACCOUNTS("a", "accounts", true, "name of an account file");
	

	public String shortName;
	public String longName;
	public String description;
	public boolean hasValue;

	private CliOptions(String shortName, String longName, boolean hasValue, String description) {
		this.shortName = shortName;
		this.longName = longName;
		this.description = description;
		this.hasValue = hasValue;
	}
	
	public Option asOption(){
		return new Option(shortName, longName, hasValue, description);
	}

}
