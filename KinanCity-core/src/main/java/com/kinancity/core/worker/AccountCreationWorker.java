package com.kinancity.core.worker;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.PtcSession;
import com.kinancity.api.captcha.CaptchaProvider;
import com.kinancity.api.errors.FatalException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.fatal.AccountDuplicateException;
import com.kinancity.api.errors.fatal.EmailDuplicateOrBlockedException;
import com.kinancity.api.errors.tech.AccountRateLimitExceededException;
import com.kinancity.api.errors.tech.CaptchaSolvingException;
import com.kinancity.api.model.AccountData;
import com.kinancity.core.model.AccountCreation;
import com.kinancity.core.model.CreationFailure;
import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.proxy.ProxyManager;
import com.kinancity.core.scheduling.AccountCreationQueue;
import com.kinancity.core.status.ErrorCode;
import com.kinancity.core.status.RunnerStatus;
import com.kinancity.core.worker.callbacks.CreationCallbacks;

import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;

/**
 * Runnable class that will be ran as a Thread and create the accounts in queue.
 * 
 * @author drallieiv
 *
 */
@Getter
public class AccountCreationWorker implements Runnable {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private AccountCreationQueue accountCreationQueue;

	private CreationCallbacks callbacks;

	private String name;

	private RunnerStatus status;

	/**
	 * This status is never updated to something else then STOP in the run loop
	 */
	private RunnerStatus loopStatus;

	private AccountCreation currentCreation;

	/**
	 * If true then the thread execution will stop once we reach end of the queue. We might want to stay idle until all other thread are done.
	 */
	@Setter
	private boolean stopWithQueueEnd = false;

	/**
	 * How fast does idle thread check if there is something to do in millis
	 */
	private long idlePollingRate = 1000;

	private CaptchaProvider captchaProvider;

	private ProxyManager proxyManager;

	public AccountCreationWorker(AccountCreationQueue accountCreationQueue, String name, CaptchaProvider captchaProvider, ProxyManager proxyManager, CreationCallbacks callbacks) {
		this.status = RunnerStatus.IDLE;
		this.accountCreationQueue = accountCreationQueue;
		this.name = name;
		this.callbacks = callbacks;
		this.captchaProvider = captchaProvider;
		this.proxyManager = proxyManager;
	}

	public void run() {

		// Run Loop
		loopStatus = RunnerStatus.RUNNING;

		while (loopStatus != RunnerStatus.STOP) {
			// Pull an account to create
			currentCreation = accountCreationQueue.pop();

			if (currentCreation != null) {
				status = RunnerStatus.RUNNING;

				// Start account creation
				AccountData account = currentCreation.getAccountData();
				logger.info("Create account : {}", account);

				try {
					// Grab an available connection
					ProxyInfo proxy = getAvailableProxy();
					logger.debug("Use proxy : {}", proxy);

					try {
						// Get a new HTTP Client instance with that proxy
						OkHttpClient httclient = proxy.getProvider().getClient();

						// Initialize a PTC Creation session
						PtcSession ptc = new PtcSession(httclient);

						// 1. Check password and username before we start
						if (ptc.isAccountValid(account)) {
							// 2. Start session
							String crsfToken = ptc.sendAgeCheckAndGrabCrsfToken();

							// 3. Captcha
							String captcha = captchaProvider.getCaptcha();

							// 4. Account Creation
							ptc.createAccount(account, crsfToken, captcha);

							// All OK
							callbacks.onSuccess(currentCreation);

						} else {
							currentCreation.getFailures().add(new CreationFailure(ErrorCode.BAD_USERNAME_OR_PASSWORD));
							callbacks.onFailure(currentCreation);
						}
					} catch (CaptchaSolvingException e) {
						logger.warn("Captcha solving failed");
						currentCreation.getFailures().add(new CreationFailure(ErrorCode.CAPTCHA_SOLVING));
						callbacks.onTechnicalIssue(currentCreation);

						// Do not count that try on proxy limitation
						proxy.freeOneTry();
					} catch (TechnicalException e) {
						logger.warn("Technical Error : {}", e.getMessage());
						currentCreation.getFailures().add(new CreationFailure(ErrorCode.TECH_ERROR, e.getMessage(), e));
						callbacks.onTechnicalIssue(currentCreation);

						if (e instanceof AccountRateLimitExceededException) {
							proxy.getProxyPolicy().markOverLimit();
						} else {
							// Do not count that try on proxy limitation
							proxy.freeOneTry();
						}

					} catch (FatalException e) {
						logger.error("Fatal Error : {}", e.getMessage());
						if (e instanceof AccountDuplicateException) {
							currentCreation.getFailures().add(new CreationFailure(ErrorCode.ACCOUNT_DUPLICATE, e.getMessage(), e));
						} else if (e instanceof EmailDuplicateOrBlockedException) {
							currentCreation.getFailures().add(new CreationFailure(ErrorCode.EMAIL_DUPLICATE, e.getMessage(), e));
						} else {
							currentCreation.getFailures().add(new CreationFailure(ErrorCode.FATAL_ERROR, e.getMessage(), e));
						}

						callbacks.onFailure(currentCreation);

						// Do not count that try on proxy limitation
						proxy.freeOneTry();
					}
				} catch (TechnicalException e) {
					// Error when getAvailableProxy
					logger.warn("Technical Error while getting proxy : {}", e.getMessage());
					currentCreation.getFailures().add(new CreationFailure(ErrorCode.TECH_ERROR, e.getMessage(), e));
					callbacks.onTechnicalIssue(currentCreation);
				}

			} else if (stopWithQueueEnd) {
				status = RunnerStatus.STOP;
				loopStatus = RunnerStatus.STOP;
			} else {
				status = RunnerStatus.IDLE;
				try {
					Thread.sleep(idlePollingRate);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}

	/**
	 * Get an available proxy, or wait if none
	 * 
	 * @return
	 * @throws TechnicalException
	 * @throws InterruptedException
	 */
	private ProxyInfo getAvailableProxy() throws TechnicalException {
		try {
			Optional<ProxyInfo> proxyInfo = proxyManager.getEligibleProxy();
			if (!proxyInfo.isPresent()) {
				logger.info("No proxy available for now. Start waiting for one.");
				while (!proxyInfo.isPresent()) {
					Thread.sleep(proxyManager.getPollingRate());
					proxyInfo = proxyManager.getEligibleProxy();
				}
			}

			return proxyInfo.get();

		} catch (InterruptedException e) {
			throw new TechnicalException("Interrupted when waiting for proxy", e);
		}
	}

	public void stop() {
		this.loopStatus = RunnerStatus.STOP;
	}
}
