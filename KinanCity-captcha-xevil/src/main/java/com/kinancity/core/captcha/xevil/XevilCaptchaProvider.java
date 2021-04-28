package com.kinancity.core.captcha.xevil;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;

import lombok.Setter;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class XevilCaptchaProvider extends CaptchaProvider {

	private CaptchaQueue queue;

	/**
	 * List of 2captcha Challenges sent
	 */
	private List<XevilChallenge> challenges = new ArrayList<>();

	private boolean runFlag = true;

	/**
	 * Actions that can be asked
	 */
	private static final String ACTION_GET = "get";
	private static final String ACTION_GETBALANCE = "getbalance";

	@Setter
	private String captchaBase = "http://2captcha.com/";
	private static final String CAPTCHA_IN = "in.php";
	private static final String CAPTCHA_OUT = "res.php";

	private static final String HTTP_ERROR_MSG = "Could not reach Xevil service";

	private OkHttpClient captchaClient;

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Captcha API key
	 */
	private String apiKey = null;

	/**
	 * 2 captcha Soft Id;
	 */
	private String softId = "1816";

	/**
	 * Wait at least that time (in seconds) before sending first resolve request. (default 5s)
	 */
	@Setter
	private int minTimeBeforeFirstResolve = 5;

	/**
	 * Google Site key
	 */
	private String googleSiteKey = "6LdpuiYTAAAAAL6y9JNUZzJ7cF3F8MQGGKko1bCy";

	/**
	 * Signup URL
	 */
	private static final String PAGE_URL = "https://club.pokemon.com/us/pokemon-trainer-club/parents/sign-up";

	/**
	 * List of 2 captcha Response codes we use
	 */
	public static final String CAPCHA_NOT_READY = "CAPCHA_NOT_READY";
	public static final String OK_RESPONSE_PREFIX = "OK|";
	// Wrong "key" parameter format, it should contain 32 symbols
	public static final String ERROR_WRONG_USER_KEY = "ERROR_WRONG_USER_KEY";

	/**
	 * How often should we call Xevil (default 5000 ms)
	 */
	@Setter
	private int waitBeforeRetry = 5000;

	@Setter
	private int maxPerLoop = 5;

	public static CaptchaProvider getInstance(CaptchaQueue queue, Properties prop) throws CaptchaException {
		return new XevilCaptchaProvider(queue, prop.getProperty("captcha.key", ""), prop.getProperty("captcha.url", null));
	}

	public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey, String captchaBase) throws CaptchaException {
		return new XevilCaptchaProvider(queue, apiKey, captchaBase);
	}

	public XevilCaptchaProvider(CaptchaQueue queue, String apiKey, String captchaBase) throws CaptchaException {
		this.queue = queue;
		this.apiKey = apiKey;
		this.captchaClient = new OkHttpClient();
		if (captchaBase != null) {
			if (!captchaBase.endsWith("/")) {
				captchaBase = captchaBase + "/";
			}
			this.captchaBase = captchaBase;
		}
	}

	@Override
	public void run() {

		while (runFlag) {

			LocalDateTime minDate = LocalDateTime.now().minusSeconds(minTimeBeforeFirstResolve);
			Set<XevilChallenge> challengesToResolve = challenges.stream().filter(c -> c.getSentTime().isBefore(minDate)).collect(Collectors.toSet());

			if (challengesToResolve.isEmpty()) {
				// No captcha waiting
				logger.debug("No captcha check needed, {} in queue", challenges.size());
			} else {
				// Currently waiting for captcha
				logger.debug("Check status of {} captchas", challengesToResolve.size());

				for (XevilChallenge challenge : challengesToResolve) {
					String captchaId = challenge.getCaptchaId();
					logger.debug("Check captcha {}", captchaId);

					Request resolveRequest = buildReceiveCaptchaRequest(captchaId);

					try (Response solveResponse = captchaClient.newCall(resolveRequest).execute()) {
						String body = solveResponse.body().string();

						manageChallengeResponse(challenge, body);
					} catch (IOException e) {
						logger.error("Error while calling RES : {}", e.getMessage());
					}
				}

			}

			// Update queue size

			// Number of elements waiting for a captcha
			int nbInQueue = queue.size();

			// Number currently waiting at 2captcha
			int nbWaiting = challenges.size();

			// How many more do we need
			int nbNeeded = Math.min(nbInQueue, getMaxParallelChallenges());

			int nbToRequest = Math.max(0, Math.min(nbNeeded - nbWaiting, maxPerLoop));
			if (nbToRequest > 0) {

				logger.info("Requested {} new Captchas", nbToRequest);

				// Send new captcha requests
				Request sendRequest = buildSendCaptchaRequest();
				for (int i = 0; i < nbToRequest; i++) {
					try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
						String body = sendResponse.body().string();

						if (isValidResponse(body)) {
							String captchaId = body.split("\\|")[1];
							logger.info("Requested new Captcha, id : {}", captchaId);
							challenges.add(new XevilChallenge(captchaId));
						} else {
							logger.error("KO response when sending IN captcha provider : {}", body);
						}

					} catch (IOException e) {
						logger.error("Error while calling IN 2captcha : {}", e.getMessage());
					}
				}
			}

			try {
				Thread.sleep(waitBeforeRetry);
			} catch (InterruptedException e) {
				logger.error("Interrupted");
			}
		}
	}

	/**
	 * Update the challenge according to given response
	 * 
	 * @param challenge
	 * @param response
	 */
	public void manageChallengeResponse(XevilChallenge challenge, String response) {

		if (CAPCHA_NOT_READY.equals(response)) {
			// Captcha not ready, should wait
			if (getMaxWait() > 0 && LocalDateTime.now().isAfter(challenge.getSentTime().plusSeconds(getMaxWait()))) {
				logger.error("This captcha has been waiting too long, drop it. Increase `maxWait` if that happens too often");
				challenges.remove(challenge);
			} else {
				// Keep the challenge in queue
				challenge.setNbPolls(challenge.getNbPolls() + 1);
			}

		} else if (response.contains("ERROR_")) {
			logger.error("This captcha had an unexpected error : {}", response);
			challenges.remove(challenge);
		} else {
			logger.info("Captcha response given in {}s", ChronoUnit.SECONDS.between(challenge.getSentTime(), LocalDateTime.now()));
			queue.addCaptcha(response);
			challenges.remove(challenge);
		}
	}

	private boolean isValidResponse(String body) {
		return body.startsWith("OK|");
	}

	@Override
	public double getBalance() throws CaptchaException, TechnicalException {
		Request sendRequest = buildBalanceCheckequest();

		try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
			String body = sendResponse.body().string();

			if (body == null) {
				throw new TechnicalException("invalid response from Captcha provider");
			}

			// Check error results
			if (body.startsWith(ERROR_WRONG_USER_KEY)) {
				throw new XevilCaptchaConfigurationException("Given captcha key " + apiKey + " is invalid");
			}
			/*
			 * if (body.startsWith(ERROR_KEY_DOES_NOT_EXIST)) { throw new XevilCaptchaConfigurationException("Given 2captcha key does not match any account"); }
			 * 
			 * if (body.startsWith(ERROR_PREFIX)) { throw new XevilCaptchaConfigurationException("Returned 2captcha Error : " + body); }
			 */

			try {
				return Double.parseDouble(body);
			} catch (NumberFormatException e) {
				throw new XevilCaptchaConfigurationException("Invalid balance call return : " + body);
			}

		} catch (IOException e) {
			throw new TechnicalException(HTTP_ERROR_MSG, e);
		}
	}

	/**
	 * Send Captcha Request
	 * 
	 * @return
	 */
	private Request buildSendCaptchaRequest() {
		HttpUrl url = HttpUrl.parse(captchaBase + CAPTCHA_IN).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("method", "userrecaptcha")
				.addQueryParameter("googlekey", googleSiteKey)
				.addQueryParameter("pageurl", PAGE_URL)
				.addQueryParameter("soft_id", softId)
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
		HttpUrl url = HttpUrl.parse(captchaBase + CAPTCHA_OUT).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("action", ACTION_GET)
				.addQueryParameter("ids", catpchaId)
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
		HttpUrl url = HttpUrl.parse(captchaBase + CAPTCHA_OUT).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("action", ACTION_GETBALANCE)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.build();
		return request;
	}
}
