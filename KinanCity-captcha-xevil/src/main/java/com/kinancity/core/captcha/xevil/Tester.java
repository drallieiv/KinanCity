package com.kinancity.core.captcha.xevil;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.CaptchaRequest;
import com.kinancity.core.captcha.impl.LogCaptchaCollector;

public class Tester {

	private static Logger logger = LoggerFactory.getLogger(Tester.class);

	public static void main(String[] args) throws CaptchaException, TechnicalException, InterruptedException {

		Properties prop = new Properties();
		prop.setProperty("captcha.url", args[0]);

		CaptchaQueue queue = new CaptchaQueue(new LogCaptchaCollector());

		CaptchaProvider provider = XevilCaptchaProvider.getInstance(queue, prop);

		logger.info("Start Provider");
		new Thread(provider).start();

		logger.info("Provider Started, get Balance");

		double balance = provider.getBalance();
		logger.info("Balance is : {}", balance);

		while (true) {
			logger.info("Add 2 requests to queue");
			CaptchaRequest request = queue.addRequest(new CaptchaRequest("test1"));
			CaptchaRequest request2 = queue.addRequest(new CaptchaRequest("test2"));

			while (request.getResponse() == null || request2.getResponse() == null) {
				Thread.sleep(500);
			}

			logger.info("Response given : {}", request.getResponse());
			logger.info("Response given : {}", request2.getResponse());

			Thread.sleep(5000);
		}
	}

}
