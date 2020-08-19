package com.kinancity.core.captcha.xevil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.CaptchaRequest;
import com.kinancity.core.captcha.impl.LogCaptchaCollector;

public class XevilCaptchaProviderTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Properties prop;

	@Test
	public void XevilSolvingTest() throws CaptchaException, TechnicalException, InterruptedException {

		CaptchaQueue queue = new CaptchaQueue(new LogCaptchaCollector());

		CaptchaProvider provider = XevilCaptchaProvider.getInstance(queue, prop);

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

	@Before
	public void loadConfig() throws IOException {
		prop = new Properties();
		File configFile = new File("config.properties");

		InputStream in = new FileInputStream(configFile);
		prop.load(in);
		in.close();
	}
}
