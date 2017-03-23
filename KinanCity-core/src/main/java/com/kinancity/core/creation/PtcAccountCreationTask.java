package com.kinancity.core.creation;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.Configuration;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.data.AccountData;
import com.kinancity.core.errors.AccountCreationException;
import com.kinancity.core.proxy.HttpProxyProvider;
import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.proxy.ProxyManager;

/**
 * Runnable account creation task
 * 
 * @author drallieiv
 *
 */
public class PtcAccountCreationTask implements Callable<PtcCreationResult> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private AccountData account;

	private CaptchaProvider captchaProvider;
	
	private ProxyManager proxyManager;
	
	private Configuration config;

	public PtcAccountCreationTask(AccountData account, Configuration config) {
		super();
		this.config = config;
		this.proxyManager = config.getProxyManager();
		this.captchaProvider = config.getCaptchaProvider();
		this.account = account;
	}

	@Override
	public PtcCreationResult call() throws AccountCreationException {

		try {
			// Try to get an available proxy
			Optional<ProxyInfo> proxyInfo = proxyManager.getEligibleProxy();
			if(!proxyInfo.isPresent()){
				logger.info("No proxy available for now. Start waiting for one.");
				while (! proxyInfo.isPresent()){
					Thread.sleep(proxyManager.getPollingRate());
					proxyInfo = proxyManager.getEligibleProxy();
				}
			}
			
			ProxyInfo usingProxy = proxyInfo.get();

			logger.debug("Use proxy : {}", usingProxy);
					
			PtcWebClient client = new PtcWebClient(config, usingProxy);
					
			logger.info("Create account with {}", account);
			
			// 1. password and name check ?
			if (!client.validateAccount(account)) {
				// As it would not count in the limit we can remove one
				usingProxy.freeOneTry();
				return new PtcCreationResult(false, "Invalid username or already taken", null);
			}

			// 2. Grab a CRSF token
			String crsfToken = client.sendAgeCheckAndGrabCrsfToken();
			if (crsfToken == null) {
				AccountCreationException error = new AccountCreationException("Could not grab CRSF token. pokemon-trainer-club website may be unavailable");
				// As it would not count in the limit we can remove one
				usingProxy.freeOneTry();
				return new PtcCreationResult(false, "CRSF failed", error);
			}
			logger.debug("CRSF token found : {}", crsfToken);

			// 3. Captcha
			String captcha = captchaProvider.getCaptcha();

			// 4. Account Creation
			client.createAccount(account, crsfToken, captcha);
					
			config.getResultLogWriter().println(account.toCsv());
			config.getResultLogWriter().flush();
			
			return new PtcCreationResult(true, "Account created", null);
		} catch (InterruptedException e) {
			throw new AccountCreationException(e);
		}
	}

}
