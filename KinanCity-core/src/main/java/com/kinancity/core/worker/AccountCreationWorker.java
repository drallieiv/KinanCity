package com.kinancity.core.worker;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.PtcSession;
import com.kinancity.api.errors.FatalException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.fatal.AccountDuplicateException;
import com.kinancity.api.errors.fatal.EmailDuplicateOrBlockedException;
import com.kinancity.api.errors.tech.AccountRateLimitExceededException;
import com.kinancity.api.errors.tech.CaptchaSolvingException;
import com.kinancity.api.errors.tech.HttpConnectionException;
import com.kinancity.api.errors.tech.IpSoftBanException;
import com.kinancity.api.model.AccountData;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.CaptchaRequest;
import com.kinancity.core.model.AccountCreation;
import com.kinancity.core.model.CreationFailure;
import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.proxy.ProxyManager;
import com.kinancity.core.proxy.ProxySlot;
import com.kinancity.core.scheduling.AccountCreationQueue;
import com.kinancity.core.status.ErrorCode;
import com.kinancity.core.status.RunnerStatus;
import com.kinancity.core.throttle.Bottleneck;
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

	private CaptchaQueue captchaQueue;

	private ProxyManager proxyManager;

	@Setter
	private boolean dryRun = false;

	@Setter
	private int dumpResult = PtcSession.NEVER;

	// Wait at least this time before requesting a captcha
	@Setter
	private long minWaitBeforeCaptcha = 5000;
	
	private Bottleneck<ProxyInfo> bottleneck;

	public AccountCreationWorker(AccountCreationQueue accountCreationQueue, String name, CaptchaQueue captchaQueue, ProxyManager proxyManager, CreationCallbacks callbacks, Bottleneck<ProxyInfo> bottleneck) {
		this.status = RunnerStatus.IDLE;
		this.accountCreationQueue = accountCreationQueue;
		this.name = name;
		this.callbacks = callbacks;
		this.captchaQueue = captchaQueue;
		this.proxyManager = proxyManager;
		this.bottleneck = bottleneck;
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
					ProxySlot proxySlot = getAvailableProxy();
					ProxyInfo proxy = proxySlot.getInfo();
					logger.debug("Use proxy : {}", proxy);

					OkHttpClient httpclient = null;
					try {
						// Get a new HTTP Client instance with that proxy
						httpclient = proxy.getProvider().getClient();

						// Initialize a PTC Creation session
						PtcSession ptc = new PtcSession(httpclient);
						ptc.setDryRun(dryRun);
						ptc.setDumpResult(dumpResult);

						// 1. Check password and username before we start
						if (ptc.isAccountValid(account)) {
							
							if(bottleneck != null){
								bottleneck.syncUseOf(proxy);
							}
							
							// 2A. Start session : get CRSF Token
							String crsfToken = ptc.sendAgeCheckAndGrabCrsfToken(account);
							
							if(bottleneck != null){
								bottleneck.syncUseOf(proxy);
							}
							
							// 2B. Start session : send age check
							ptc.sendAgeCheck(account, crsfToken);
							
							try {
								Thread.sleep(minWaitBeforeCaptcha);
							} catch (InterruptedException e) {
								// Interrupted
							}
							
							// 3. Captcha
							CaptchaRequest captchaRequest = new CaptchaRequest(account.getUsername());
							captchaQueue.addRequest(captchaRequest);
							String captcha = captchaRequest.getResponse();
							logger.debug("Use Captcha : {}", captcha);

							if(bottleneck != null){
								bottleneck.syncUseOf(proxy);
							}
							
							// 4. Account Creation
							proxySlot.markUsed();
							ptc.createAccount(account, crsfToken, captcha);

							// All OK
							callbacks.onSuccess(currentCreation);

						} else {
							currentCreation.getFailures().add(new CreationFailure(ErrorCode.BAD_USERNAME_OR_PASSWORD));
							callbacks.onFailure(currentCreation);
						}
					} catch (IpSoftBanException e){
						if(bottleneck != null){
							// Send error to the bottleneck too
							logger.warn("PTC softban, put that IP on hold. host:{}", proxy.getProvider().getHost());
							bottleneck.onServerError(proxy);
						}
						callbacks.onTechnicalIssue(currentCreation);
						
						// Free that proxy slot for re-use
						proxySlot.freeSlot();
					} catch (CaptchaSolvingException e) {
						logger.warn(e.getMessage());
						currentCreation.getFailures().add(new CreationFailure(ErrorCode.CAPTCHA_SOLVING));
						callbacks.onTechnicalIssue(currentCreation);

						// Free that proxy slot for re-use
						proxySlot.freeSlot();
					} catch (HttpConnectionException e) {
						logger.warn("HttpConnectionException, proxy [{}] might be bad, move it out of rotation : {}", proxy.getProvider(), e.getMessage());
						proxyManager.benchProxy(proxy);

						currentCreation.getFailures().add(new CreationFailure(ErrorCode.NETWORK_ERROR, e.getMessage(), e));
						callbacks.onTechnicalIssue(currentCreation);

						// Free that proxy slot for re-use
						proxySlot.freeSlot();
					} catch (TechnicalException e) {
						if(e.getCause() != null){
							logger.warn("Technical Error : {} caused by {} ", e.getMessage(), e.getCause());
						}else{
							logger.warn("Technical Error : {}", e.getMessage());
						}						
						currentCreation.getFailures().add(new CreationFailure(ErrorCode.TECH_ERROR, e.getMessage(), e));
						callbacks.onTechnicalIssue(currentCreation);

						if (e instanceof AccountRateLimitExceededException) {
							proxy.getProxyPolicy().markOverLimit();
						} else {
							// Free that proxy slot for re-use
							proxySlot.freeSlot();
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

						// Free that proxy slot for re-use
						proxySlot.freeSlot();
					} finally {
						if(httpclient != null){
							httpclient.connectionPool().evictAll();
						}
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
				logger.info("End of account creation queue reached. Stop worker");
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
	private ProxySlot getAvailableProxy() throws TechnicalException {
		try {
			Optional<ProxySlot> proxyInfo = proxyManager.getEligibleProxy();
			if (!proxyInfo.isPresent()) {
				logger.info(
						"No proxy slots available for now. Start waiting for one. {} proxies in rotation and {} benched.", 
						proxyManager.getNbProxyInRotation(), 
						proxyManager.getNbProxyBenched());
				while (!proxyInfo.isPresent()) {
					Thread.sleep(proxyManager.getPollingRate());
					proxyInfo = proxyManager.getEligibleProxy();
					if(!proxyInfo.isPresent()){
						logger.info("Still no proxy slots available. Keep waiting");
					}
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
