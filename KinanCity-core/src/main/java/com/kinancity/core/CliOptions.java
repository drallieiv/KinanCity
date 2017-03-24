package com.kinancity.core;

import org.apache.commons.cli.Option;

import lombok.Getter;

@Getter
public enum CliOptions {

	DRY_RUN("dryrun", "dryrun", false, "Dry-Run"),
	
	CK("ck", "captchaKey", true, "2 captcha API Key"),

	EMAIL("m", "mail", true, "account email address"),
	SINGLE_USERNAME("u", "username", true, "account username/login"),
	PASSWORD("p", "password", true, "account password"),
	
	SEQ_ACCOUNTS_COUNT("c", "count", true, "number of accounts to generate"),
	SEQ_ACCOUNTS_START("s", "startnum", true, "number of the first one"),
	SEQ_ACCOUNTS_FORMAT("f", "format", true, "format of the username : prefix*****suffix with enough *"),
	
	PROXIES("px", "proxies", true, "single proxy or list of [proxy1,proxy2]"),
	
	OUTPUT("o", "output", true, "output csv file path where the result should be saved"),
	
	NO_PROXY_CHECK("npc", "noProxyCheck", false, "Skip Proxy Checking"),
	NB_THREADS("t", "thread", true, "Number of parallel threads"),
	
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
