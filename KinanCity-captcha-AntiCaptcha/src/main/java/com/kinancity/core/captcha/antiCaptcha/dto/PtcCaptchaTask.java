package com.kinancity.core.captcha.antiCaptcha.dto;

public class PtcCaptchaTask extends AbstractTaskDto{

	public static final String GOOGLE_SITE_KEY = "6LdpuiYTAAAAAL6y9JNUZzJ7cF3F8MQGGKko1bCy";
	public static final String PAGE_URL = "https://club.pokemon.com/us/pokemon-trainer-club/parents/sign-up";
	public static final String TYPE = "NoCaptchaTaskProxyless";
	
	public PtcCaptchaTask() {
		this.setType(TYPE);
		this.setWebsiteKey(GOOGLE_SITE_KEY);
		this.setWebsiteURL(PAGE_URL);
	}

}
