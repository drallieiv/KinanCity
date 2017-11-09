package com.kinancity.captcha.client;

import org.junit.Ignore;
import org.junit.Test;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.CaptchaRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientProviderTest {

	@Ignore
	@Test
	public void startupTest() throws CaptchaException, TechnicalException {
		CaptchaQueue queue = new CaptchaQueue();

		ClientProvider provider = new ClientProvider(queue);
		provider.setCaptchaBalanceUrl("http://localhost:8888/captcha/balance");
		provider.setCaptchaSubmitUrl("http://localhost:8888/captcha/submit");
		provider.setCaptchaRetrieveUrl("http://localhost:8888/captcha/retrieive");

		double balance = provider.getBalance();
		log.info("Balance retrieved : {}", balance);

		queue.addRequest(new CaptchaRequest("test"));

		provider.run();
	}

}
