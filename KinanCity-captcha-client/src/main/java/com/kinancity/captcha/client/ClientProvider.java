package com.kinancity.captcha.client;

import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.antiCaptcha.AntiCaptchaProvider;

public class ClientProvider extends AntiCaptchaProvider {

	private static final String CAPTCHA_RETRIEIVE_URL = "http://localhost:8888/captcha/retrieive";
	private static final String CAPTCHA_SUBMIT_URL = "http://localhost:8888/captcha/submit";
	private static final String CAPTCHA_BALANCE_URL = "http://localhost:8888/captcha/balance";

	public static CaptchaProvider getInstance(CaptchaQueue queue) throws CaptchaException {
		ClientProvider provider = new ClientProvider(queue);
		provider.setCaptchaBalanceUrl(CAPTCHA_BALANCE_URL);
		provider.setCaptchaSubmitUrl(CAPTCHA_SUBMIT_URL);
		provider.setCaptchaRetrieveUrl(CAPTCHA_RETRIEIVE_URL);
		provider.setWaitBeforeRetry(200);
		return provider;
	}

	public ClientProvider(CaptchaQueue queue) throws CaptchaException {
		super(queue, "NONE");
	}

}
