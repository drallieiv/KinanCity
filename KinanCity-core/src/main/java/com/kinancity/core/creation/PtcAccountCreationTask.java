package com.kinancity.core.creation;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.Configuration;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.data.AccountData;
import com.kinancity.core.errors.AccountCreationException;

/**
 * Runnable account creation task
 * 
 * @author drallieiv
 *
 */
public class PtcAccountCreationTask implements Callable<PtcCreationResult> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private AccountData account;

	private PtcWebClient client;

	private Configuration config;

	private CaptchaProvider captchaProvider;

	// Dry Run
	private boolean dryRun;

	public PtcAccountCreationTask(AccountData account, Configuration config, CaptchaProvider captchaProvider) {
		super();
		this.client = new PtcWebClient(config);
		this.config = config;
		this.captchaProvider = captchaProvider;
		this.dryRun = config.isDryRun();
		this.account = account;
	}


	@Override
	public PtcCreationResult call() throws AccountCreationException {
		logger.info("Create account with username {}", account);

		// 0. password and name check ?
		if (!client.validateAccount(account)) {
			logger.info("Invalid account will be skipped");
			return new PtcCreationResult(false, "Invalid account", null);
		}

		// 1. Grab a CRSF token
		String crsfToken = client.sendAgeCheckAndGrabCrsfToken();
		if (crsfToken == null) {
			AccountCreationException error = new AccountCreationException("Could not grab CRSF token. pokemon-trainer-club website may be unavailable");
			return new PtcCreationResult(false, "CRSF failed", error);
		}
		logger.debug("CRSF token found : {}", crsfToken);

		// 3. Captcha
		String captcha = captchaProvider.getCaptcha();

		// 4. Account Creation
		client.createAccount(account, crsfToken, captcha);
		
		return new PtcCreationResult(true, "Account created", null);
	}

}
