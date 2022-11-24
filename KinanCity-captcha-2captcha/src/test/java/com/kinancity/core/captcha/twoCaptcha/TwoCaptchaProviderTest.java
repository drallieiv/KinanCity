package com.kinancity.core.captcha.twoCaptcha;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.kinancity.core.captcha.CaptchaProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.CaptchaRequest;
import com.kinancity.core.captcha.impl.LogCaptchaCollector;

public class TwoCaptchaProviderTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Ignore
	@Test
	public void solvingTest() throws CaptchaException, TechnicalException, InterruptedException {

		CaptchaQueue queue = new CaptchaQueue(new LogCaptchaCollector());

		String apiKey = System.getenv("apiKey") ;
		String altUrl = System.getenv("altUrl") ;

		CaptchaProvider provider = TwoCaptchaProvider.getInstance(queue, apiKey, altUrl);

		logger.info("Start Provider");
		new Thread(provider).start();

		logger.info("Provider Started, get Balance");

		double balance = provider.getBalance();
		logger.info("Balance is : {}", balance);

		CaptchaRequest request = queue.addRequest(new CaptchaRequest("test1"));
		CaptchaRequest request2 = queue.addRequest(new CaptchaRequest("test2"));

		while (request.getResponse() == null || request2.getResponse() == null) {
			Thread.sleep(500);
		}

		logger.info("Response 1 given : {}", request.getResponse());
		logger.info("Response 2 given : {}", request2.getResponse());

		Thread.sleep(2000);

	}

}
