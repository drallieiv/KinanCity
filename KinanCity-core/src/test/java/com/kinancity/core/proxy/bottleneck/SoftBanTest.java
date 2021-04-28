package com.kinancity.core.proxy.bottleneck;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.PtcSession;
import com.kinancity.api.errors.FatalException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.fatal.EmailDuplicateOrBlockedException;
import com.kinancity.api.errors.tech.IpSoftBanException;
import com.kinancity.api.model.AccountData;
import com.kinancity.core.proxy.impl.NoProxy;
import com.kinancity.core.throttle.bottleneck.BottleneckCooldown;

import okhttp3.OkHttpClient;

public class SoftBanTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	@Ignore
	public void softBanTest() {

		OkHttpClient httclient = (new NoProxy()).getClient();
		BottleneckCooldown<String> bottleneck = new BottleneckCooldown<String>();
		new Thread(bottleneck).start();
		String bottleneckName = "test";
		AccountData account = new AccountData("test", "test@test.com", "PassW0rd+");

		for (int i = 0; i < 20; i++) {

			logger.debug("Start another account creation");

			try {
				// Initialize a PTC Creation session
				PtcSession ptc = new PtcSession(httclient);

				logger.debug("Bottleneck check 1");
				bottleneck.syncUseOf(bottleneckName);
				logger.debug("Bottleneck check 1 OK");

				// 2. Start session
				String crsfToken = ptc.sendAgeCheckAndGrabCrsfToken(account);

				logger.debug("Bottleneck check 2");
				bottleneck.syncUseOf(bottleneckName);
				logger.debug("Bottleneck check 2 OK");
				ptc.sendAgeCheck(account, crsfToken);

				String captcha = "fakeCaptcha";

				logger.debug("Bottleneck check 3");
				bottleneck.syncUseOf(bottleneckName);
				logger.debug("Bottleneck check 3 OK");

				ptc.createAccount(account, crsfToken, captcha);

			} catch (EmailDuplicateOrBlockedException e) {
				// Duplicate expected
			} catch (IpSoftBanException e) {
				// fail("503 error");
				logger.error("IpSoftBanException wait 60s");
				try {
					Thread.sleep(60000);
				} catch (InterruptedException e1) {
					// Sleep
				}
				logger.info("60s over, retry");
			} catch (TechnicalException e) {
				// Technical Exception
				logger.warn(e.getMessage());
			} catch (FatalException e) {
				// Fatal Exception
				logger.warn(e.getMessage());
			}
		}

	}

}
