package com.kinancity.core.creation;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.Configuration;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.data.AccountData;
import com.kinancity.core.errors.AccountCreationException;
import com.kinancity.core.errors.CaptchaSolvingException;
import com.kinancity.core.errors.FatalException;
import com.kinancity.core.errors.TechnicalException;
import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.proxy.ProxyManager;

import lombok.Getter;

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

	private PtcAccountCreator ptcAccountCreator;

	private int nbtry = 1;

	public PtcAccountCreationTask(AccountData account, Configuration config, PtcAccountCreator ptcAccountCreator) {
		this(account, config, ptcAccountCreator, 1);
	}

	public PtcAccountCreationTask(AccountData account, Configuration config, PtcAccountCreator ptcAccountCreator, int nbtry) {
		super();
		this.config = config;
		this.proxyManager = config.getProxyManager();
		this.captchaProvider = config.getCaptchaProvider();
		this.account = account;
		this.nbtry = nbtry;
		this.ptcAccountCreator = ptcAccountCreator;
	}

	@Override
	public PtcCreationResult call() {

		ProxyInfo usingProxy = null;
		try {
			// Try to get an available proxy
			Optional<ProxyInfo> proxyInfo = proxyManager.getEligibleProxy();
			if (!proxyInfo.isPresent()) {
				logger.info("No proxy available for now. Start waiting for one.");
				while (!proxyInfo.isPresent()) {
					Thread.sleep(proxyManager.getPollingRate());
					proxyInfo = proxyManager.getEligibleProxy();
				}
			}

			usingProxy = proxyInfo.get();

			logger.debug("Use proxy : {}", usingProxy);

			PtcWebClient client = new PtcWebClient(config, usingProxy);

			logger.info("Create account with {} try #{}", account, nbtry);

			// 1. password and name check ?
			if (!client.validateAccount(account)) {
				// As it would not count in the limit we can remove one
				usingProxy.freeOneTry();
				return new PtcCreationResult(false, "Invalid username or already taken", null);
			}

			// 2. Grab a CRSF token
			String crsfToken = client.sendAgeCheckAndGrabCrsfToken();
			logger.debug("CRSF token found : {}", crsfToken);

			// 3. Captcha
			String captcha = captchaProvider.getCaptcha();

			// 4. Account Creation
			client.createAccount(account, crsfToken, captcha);

			config.getResultLogWriter().println(account.toCsv());
			config.getResultLogWriter().flush();

			return new PtcCreationResult(true, "Account created", null);
		} catch (CaptchaSolvingException e) {
			reSchedule(usingProxy);
			return new PtcCreationResult(false, "Catpcha Failed", e);
		} catch (InterruptedException e) {
			return new PtcCreationResult(false, "Interrupted", new AccountCreationException(e));
		} catch (TechnicalException e) {
			// Something wrong happend but we can try again.
			logger.warn("There was an error but we can try to retry");
			boolean rescheduled = reSchedule(usingProxy);
			return new PtcCreationResult(false, "Technical Exception", e, rescheduled);
		} catch (FatalException e) {
			// Something wrong happend and this account will never be okay.
			return new PtcCreationResult(false, "Fatal Exception", e);
		}
	}

	// Re Schedule the task
	public boolean reSchedule(ProxyInfo proxy) {
		if (proxy != null) {
			proxy.freeOneTry();
		}
		if (nbtry < config.getMaxRetry()) {
			ptcAccountCreator.reschedule(this);
			return true;
		}
		return false;
	}
	
	public PtcAccountCreationTask getRetry(){
		return new PtcAccountCreationTask(account, config, ptcAccountCreator, nbtry + 1);
	}
}
