package com.kinancity.captcha.client;

import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.antiCaptcha.AntiCaptchaProvider;

public class ClientProvider extends AntiCaptchaProvider {

	private String captchaInUrl = "http://localhost:8888/captcha/in";
	private String captchaOutUrl = "http://localhost:8888/captcha/out";

	public static CaptchaProvider getInstance(CaptchaQueue queue) throws CaptchaException {
		return new ClientProvider(queue);
	}

	public ClientProvider(CaptchaQueue queue) throws CaptchaException {
		super(queue, "NONE");
	}

}
