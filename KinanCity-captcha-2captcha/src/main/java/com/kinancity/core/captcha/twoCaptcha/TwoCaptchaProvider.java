package com.kinancity.core.captcha.twoCaptcha;

import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;

import lombok.Getter;
import lombok.Setter;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TwoCaptchaProvider extends CaptchaProvider {

	private static final String JSON_RESPONSE = "request";

	private static final String JSON_STATUS = "status";

	private CaptchaQueue queue;

	/**
	 * List of 2captcha Challenges sent
	 */
	private List<TwoCaptchaChallenge> challenges = new ArrayList<>();

	private boolean runFlag = true;

	private static final String HTTP_ERROR_MSG = "Could not reach 2captcha servers";

	/**
	 * List of 2 captcha Response codes we use
	 */
	public static final String CAPCHA_NOT_READY = "CAPCHA_NOT_READY";
	public static final String OK_RESPONSE_PREFIX = "OK|";
	// Wrong "key" parameter format, it should contain 32 symbols
	public static final String ERROR_WRONG_USER_KEY = "ERROR_WRONG_USER_KEY";
	// The "key" doesn't exist
	public static final String ERROR_KEY_DOES_NOT_EXIST = "ERROR_KEY_DOES_NOT_EXIST";
	// You don't have money on your account
	public static final String ERROR_ZERO_BALANCE = "ERROR_ZERO_BALANCE";
	// Another Error
	public static final String ERROR_PREFIX = "ERROR_";

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
	 * 2 captcha Soft Id;
	 */
	private String softId = "1816";

	/**
	 * Wait at least that time (in seconds) before sending first resolve request. (default 5s)
	 */
	@Setter
	private int minTimeBeforeFirstResolve = 5;

	/**
	 * How often should we call 2Captcha (default 5000 ms)
	 */
	@Setter
	private int waitBeforeRetry = 5000;

	/**
	 * Google Site key
	 */
	private String googleSiteKey = "6LdpuiYTAAAAAL6y9JNUZzJ7cF3F8MQGGKko1bCy";

	// URLs
	private static final String PAGE_URL = "https://club.pokemon.com/us/pokemon-trainer-club/parents/sign-up";

	private String baseUrl = "http://2captcha.com";

	private OkHttpClient captchaClient;

	// Count the number of captcha sent
	@Getter
	private int nbSent = 0;
	
	private StopWatch recoveryModeTimer;

	// Minimum number of seconds to stay in Recovery mode
	@Setter
	private int minTimeForRecovery = 30;
	
	// Batch size if in recovery mode
	@Setter
	private int missmatchRecoverySize = 5;
	
	// Default batch size
	@Setter
	private int normalBatchSize = 12;

	public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey) throws CaptchaException {
		return new TwoCaptchaProvider(queue, apiKey, null);
	}

	public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey, String altBaseUrl) throws CaptchaException {
		return new TwoCaptchaProvider(queue, apiKey, altBaseUrl);
	}

	public TwoCaptchaProvider(CaptchaQueue queue, String apiKey, String altBaseUrl) throws CaptchaException {
		this.queue = queue;
		this.apiKey = apiKey;
		this.captchaClient = new OkHttpClient();
		if (altBaseUrl != null) {
			this.baseUrl = altBaseUrl;
		}

		if (this.apiKey == null || this.apiKey.isEmpty()) {
			throw new CaptchaException("Missing 2captcha API key");
		}
	}

	@Override
	public void run() {

		boolean missmatchRecovery = false;

		while (runFlag) {

			LocalDateTime minDate = LocalDateTime.now().minusSeconds(minTimeBeforeFirstResolve);
			Set<TwoCaptchaChallenge> challengesToResolve = challenges.stream().filter(c -> c.getSentTime().isBefore(minDate)).collect(Collectors.toSet());

			if (challengesToResolve.isEmpty()) {
				// No captcha waiting
				logger.debug("No captcha check needed, {} in queue", challenges.size());
			} else {
				// Currently waiting for captcha
				logger.debug("Check status of {} captchas", challengesToResolve.size());

				// Build a list of chucks
				List<List<TwoCaptchaChallenge>> batchList = new ArrayList<>();
				
				if (missmatchRecovery) {
					logger.info("MissmatchRecovery only check for {} captcha at once max", missmatchRecoverySize);
					batchList.add(challengesToResolve.stream().limit(missmatchRecoverySize).collect(Collectors.toList()));
				}else {
					logger.debug("Normal mode only check for {} captcha at once max", normalBatchSize);
					Set<TwoCaptchaChallenge> fullList = challengesToResolve.stream().collect(Collectors.toSet());
					batchList.addAll(ListUtils.partition(fullList.stream().collect(Collectors.toList()), normalBatchSize));
				}
				
				for (List<TwoCaptchaChallenge> batch : batchList) {
					String captchaIds = batch.stream().map(c -> c.getCaptchaId()).collect(Collectors.joining(","));
					logger.debug("captchaIds {}", captchaIds);

					Request resolveRequest = buildReceiveCaptchaRequest(captchaIds);
					
					try (Response solveResponse = captchaClient.newCall(resolveRequest).execute()) {
						String body = solveResponse.body().string();

						try {
							JsonObject jsonResponse = Json.createReader(new StringReader(body)).readObject();
							if (isValidResponse(jsonResponse)) {
								logger.debug("response : {}", body);

								String[] responses = jsonResponse.getString(JSON_RESPONSE).split("\\|");

								if (responses.length != batch.size()) {
									logger.error("Number of responses [{}] do not match number of requests [{}] : {}", responses.length, batch.size(), jsonResponse.getString(JSON_RESPONSE));
									logger.info("Switch to MissmatchRecovery mode");
									missmatchRecovery = true;
									recoveryModeTimer = new StopWatch();
									recoveryModeTimer.start();
								} else {
									if (missmatchRecovery && recoveryModeTimer.getTime() < minTimeForRecovery * 1000) {
										logger.info("Disable MissmatchRecovery mode");
										missmatchRecovery = false;
										recoveryModeTimer.stop();
									}
									int i = 0;
									for (TwoCaptchaChallenge challenge : batch) {
										String response = responses[i];
										manageChallengeResponse(challenge, response);
										i++;
									}
								}

								logger.debug("Remaining Challenges : {}", challenges.stream().map(c -> c.getCaptchaId()).collect(Collectors.joining(",")));

							} else {
								logger.error("Invalid response : {}", body);
							}
						} catch (JsonParsingException e) {
							logger.error("2captcha response was not a valid JSON : {}", body);
						}

					} catch (IOException e) {
						logger.error("Error while calling RES 2captcha : {}", e.getMessage());
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

			int nbToRequest = Math.max(0, nbNeeded - nbWaiting);
			if (nbToRequest > 0) {
				// Send new captcha requests
				Request sendRequest = buildSendCaptchaRequest();
				for (int i = 0; i < nbToRequest; i++) {
					try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
						String body = sendResponse.body().string();
						try {
							JsonObject jsonResponse = Json.createReader(new StringReader(body)).readObject();

							if (isValidResponse(jsonResponse)) {
								String captchaId = jsonResponse.getString(JSON_RESPONSE);
								logger.info("Requested new Captcha, id : {}", captchaId);
								challenges.add(new TwoCaptchaChallenge(captchaId));
							} else {
								logger.error("KO response when sending IN 2captcha : {}", body);
							}
						} catch (JsonParsingException e) {
							logger.error("2captcha response was not a valid JSON : {}", body);
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
	public void manageChallengeResponse(TwoCaptchaChallenge challenge, String response) {

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

	/**
	 * Get Current Balance. Should be called at least once before use to check for valid key.
	 * 
	 * @return current balance in USD
	 * @throws TechnicalException
	 * @throws TwoCaptchaConfigurationException
	 */
	public double getBalance() throws TechnicalException, TwoCaptchaConfigurationException {
		Request sendRequest = buildBalanceCheckequest();
		try (Response sendResponse = captchaClient.newCall(sendRequest).execute()) {
			String body = sendResponse.body().string();

			if (body == null) {
				throw new TechnicalException("invalid response from 2captcha");
			}

			// Check error results
			if (body.startsWith(ERROR_WRONG_USER_KEY)) {
				throw new TwoCaptchaConfigurationException("Given 2captcha key " + apiKey + " is invalid");
			}

			if (body.startsWith(ERROR_KEY_DOES_NOT_EXIST)) {
				throw new TwoCaptchaConfigurationException("Given 2captcha key does not match any account");
			}

			if (body.startsWith(ERROR_PREFIX)) {
				throw new TwoCaptchaConfigurationException("Returned 2captcha Error : " + body);
			}

			// Else assume we have balance
			try {
				JsonObject jsonResponse = Json.createReader(new StringReader(body)).readObject();
				if (isValidResponse(jsonResponse)) {
					try {
						return Double.parseDouble(jsonResponse.getString(JSON_RESPONSE));
					} catch (NumberFormatException e) {
						throw new TechnicalException("invalid balance from 2captcha : " + jsonResponse.getString(JSON_RESPONSE));
					}
				} else {
					throw new TechnicalException("invalid response from 2captcha : " + body);
				}
			} catch (JsonParsingException e) {
				throw new TwoCaptchaConfigurationException("Other Error : " + body);
			}

		} catch (IOException e) {
			throw new TechnicalException(HTTP_ERROR_MSG, e);
		}
	}

	public boolean isValidResponse(JsonObject jsonResponse) {
		return jsonResponse.getInt(JSON_STATUS) == 1;
	}

	/**
	 * Send Captcha Request
	 * 
	 * @return
	 */
	private Request buildSendCaptchaRequest() {
		HttpUrl url = HttpUrl.parse(getCaptchaInUrl()).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("json", "1")
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
		HttpUrl url = HttpUrl.parse(getCaptchaOutUrl()).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("json", "1")
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
		HttpUrl url = HttpUrl.parse(getCaptchaOutUrl()).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("json", "1")
				.addQueryParameter("action", ACTION_GETBALANCE)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.build();
		return request;
	}

	private String getCaptchaInUrl() {
		return this.baseUrl + "/in.php";
	}
	private String getCaptchaOutUrl() {
		return this.baseUrl + "/res.php";
	}

}
