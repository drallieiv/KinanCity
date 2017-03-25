package com.kinancity.api.captcha;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.ConfigurationException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.tech.CaptchaSolvingException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Implementation of a captcha service to 2captcha.
 * 
 * NOTE: This can be improved as 2captcha allows to ping multiple response at once.
 * 
 * @author drallieiv
 */
public class TwoCaptchaService implements CaptchaProvider {

	private static final String HTTP_ERROR_MSG = "Could not reach 2captcha servers";
	/**
	 * List of 2 captcha Response codes we use
	 */
	public static final String CAPCHA_NOT_READY = "CAPCHA_NOT_READY";
	public static final String OK_RESPONSE_PREFIX = "OK|";
	// Wrong “key” parameter format, it should contain 32 symbols
	public static final String ERROR_WRONG_USER_KEY = "ERROR_WRONG_USER_KEY";
	// The “key” doesn’t exist
	public static final String ERROR_KEY_DOES_NOT_EXIST = "ERROR_KEY_DOES_NOT_EXIST";
	// You don’t have money on your account
	public static final String ERROR_ZERO_BALANCE = "ERROR_ZERO_BALANCE";

	/**
	 * Actions that can be asked
	 */
	private static final String ACTION_GET = "get";
	private static final String ACTION_GETBALANCE = "getbalance";

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Captcha API key
	 */
	private String apiKey = null;

	/**
	 * Dry Run
	 */
	private boolean dryRun;

	/**
	 * How many time should we wait in seconds
	 */
	private int maxTotalTime = 60;

	// Wait a least 5 seconds (10-20 for recaptcha) and only then try to get the answer
	private int waitBeforeFirstTry = 15000;

	// If captcha is not solved yet - retry to get the answer after 5 seconds
	private int waitBeforeRetry = 5000;

	/**
	 * Google Site key
	 */
	private String googleSiteKey = "6LdpuiYTAAAAAL6y9JNUZzJ7cF3F8MQGGKko1bCy";

	// URLs
	private static final String PAGE_URL = "https://club.pokemon.com/us/pokemon-trainer-club/parents/sign-up";
	private static final String CAPTCHA_IN = "http://2captcha.com/in.php";
	private static final String CAPTCHA_OUT = "http://2captcha.com/res.php";

	private OkHttpClient captchaClient;

	public TwoCaptchaService(String apiKey) throws ConfigurationException, TechnicalException {
		this(apiKey, false);
	}

	public TwoCaptchaService(String apiKey, boolean dryRun) throws ConfigurationException, TechnicalException {
		this.apiKey = apiKey;
		this.dryRun = dryRun;
		this.captchaClient = new OkHttpClient();

		if (!dryRun && !checkApiKeyValidity()) {
			throw new ConfigurationException("2Captcha cannot be used with given key. Check key and balance");
		}
	}

	private boolean checkApiKeyValidity() throws TechnicalException, ConfigurationException {
		Request sendRequest = buildBalanceCheckequest();
		try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
			String body = sendResponse.body().string();

			if (body == null) {
				throw new TechnicalException("invalid response from 2captcha");
			}

			// Check error results

			if (body.startsWith(ERROR_WRONG_USER_KEY)) {
				throw new ConfigurationException("Given 2captcha key is invalid");
			}

			if (body.startsWith(ERROR_KEY_DOES_NOT_EXIST)) {
				throw new ConfigurationException("Given 2captcha key does not match any account");
			}

			// Else assume we have balance
			try {
				double balance = Double.parseDouble(body);
				if (balance < 0) {
					logger.warn("Current 2 captcha balance is negative {}", balance);
				} else {
					logger.info("Current 2 captcha balance is {}", balance);
				}
			} catch (NumberFormatException e) {
				throw new TechnicalException("invalid response from 2captcha : " + body);
			}

			return true;
		} catch (IOException e) {
			throw new TechnicalException(HTTP_ERROR_MSG, e);
		}
	}

	@Override
	public String getCaptcha() throws CaptchaSolvingException {

		if (this.apiKey == null || this.apiKey.isEmpty()) {
			throw new CaptchaSolvingException("Missing 2captcha API key");
		}

		if (dryRun) {
			logger.info("Dry-Run : Send Captcha solve request to 2captcha");
			return "mockedCaptcha";
		} else {

			Request sendRequest = buildSendCaptchaRequest();
			try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
				String body = sendResponse.body().string();

				if (body != null && body.startsWith(OK_RESPONSE_PREFIX)) {

					String captchaId = body.substring(OK_RESPONSE_PREFIX.length());

					Request resolveRequest = buildReceiveCaptchaRequest(captchaId);

					logger.info("Captcha sent to 2captcha, id: {}. Waiting for a response", captchaId);

					StopWatch time = new StopWatch();
					time.start();

					// Initial Wait
					Thread.sleep(waitBeforeFirstTry);

					while (time != null && time.getTime() < maxTotalTime * 1000) {
						try (Response solveResponse = captchaClient.newCall(resolveRequest).execute()) {
							String solution = solveResponse.body().string();

							if (solution.contains(CAPCHA_NOT_READY)) {
								// Keep Waiting
								logger.info("waiting for captcha response ...");
								Thread.sleep(waitBeforeRetry);
							} else {
								// Stop loop
								time.stop();
								logger.debug("Response received from 2captcha in {}s", time.getTime() / 1000);

								if (solution.startsWith(OK_RESPONSE_PREFIX)) {
									return solution.substring(OK_RESPONSE_PREFIX.length());
								}

								throw new CaptchaSolvingException("2 Captcha Error, solution not OK : " + solution);
							}
						}
					}
					throw new CaptchaSolvingException("2 Captcha Error, timeout reached");
				} else {
					throw new CaptchaSolvingException("2 Captcha Error : " + body);
				}
			} catch (IOException e) {
				throw new CaptchaSolvingException(e);
			} catch (InterruptedException e) {
				throw new CaptchaSolvingException(e);
			}
		}

	}

	/**
	 * Send Captcha Request
	 * 
	 * @return
	 */
	private Request buildSendCaptchaRequest() {
		HttpUrl url = HttpUrl.parse(CAPTCHA_IN).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("method", "userrecaptcha")
				.addQueryParameter("googlekey", googleSiteKey)
				.addQueryParameter("pageurl", PAGE_URL)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.build();
		return request;
	}

	/**
	 * Receive Captcha Request
	 * 
	 * @param catpchaId
	 * @return
	 */
	private Request buildReceiveCaptchaRequest(String catpchaId) {
		HttpUrl url = HttpUrl.parse(CAPTCHA_OUT).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("action", ACTION_GET)
				.addQueryParameter("id", catpchaId)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.build();
		return request;
	}

	/**
	 * Check Balance
	 * 
	 * @param catpchaId
	 * @return
	 */
	private Request buildBalanceCheckequest() {
		HttpUrl url = HttpUrl.parse(CAPTCHA_OUT).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("action", ACTION_GETBALANCE)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.build();
		return request;
	}

}
