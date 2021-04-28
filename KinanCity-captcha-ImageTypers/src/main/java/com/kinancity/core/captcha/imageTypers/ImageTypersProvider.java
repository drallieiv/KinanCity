package com.kinancity.core.captcha.imageTypers;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;

import lombok.Getter;
import lombok.Setter;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageTypersProvider extends CaptchaProvider {

	private CaptchaQueue queue;

	/**
	 * List of 2captcha Challenges sent
	 */
	private List<ImageTypersRequest> challenges = new ArrayList<>();

	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * Captcha API key
	 */
	private String apiKey = null;

	/**
	 * ImageTypers Id;
	 */
	private String softId = "535817";

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

	private boolean runFlag = true;

	private boolean stopOnInsufficientBalance = true;

	public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";

	/**
	 * Google Site key
	 */
	private String googleSiteKey = "6LdpuiYTAAAAAL6y9JNUZzJ7cF3F8MQGGKko1bCy";

	// URLs
	private static final String PAGE_URL = "https://club.pokemon.com/us/pokemon-trainer-club/parents/sign-up";

	private static String RECAPTCHA_SUBMIT_ENDPOINT = "http://captchatypers.com/captchaapi/UploadRecaptchaToken.ashx";
	private static String RECAPTCHA_RETRIEVE_ENDPOINT = "http://captchatypers.com/captchaapi/GetRecaptchaTextToken.ashx";
	private static String BALANCE_ENDPOINT = "http://captchatypers.com/Forms/RequestBalanceToken.ashx";

	private OkHttpClient captchaClient = new OkHttpClient();

	// Count the number of captcha sent
	@Getter
	private int nbSent = 0;

	public static CaptchaProvider getInstance(CaptchaQueue queue, String apiKey) throws CaptchaException {
		return new ImageTypersProvider(queue, apiKey);
	}

	public ImageTypersProvider(CaptchaQueue queue, String apiKey) throws ImageTypersConfigurationException {
		this.queue = queue;
		this.apiKey = apiKey;
		this.captchaClient = new OkHttpClient();

		if (this.apiKey == null || this.apiKey.isEmpty()) {
			throw new ImageTypersConfigurationException("Missing ImageTypers Access Token");
		}
	}

	@Override
	public void run() {

		while (runFlag) {

			LocalDateTime minDate = LocalDateTime.now().minusSeconds(minTimeBeforeFirstResolve);
			Set<ImageTypersRequest> challengesToResolve = challenges.stream().filter(c -> c.getSentTime().isBefore(minDate)).collect(Collectors.toSet());

			if (challengesToResolve.isEmpty()) {
				// No captcha waiting
				logger.debug("No captcha check needed, {} in queue", challenges.size());
			} else {
				// Currently waiting for captcha
				logger.debug("Check status of {} captchas", challengesToResolve.size());

				for (ImageTypersRequest challenge : challengesToResolve) {
					String captchaId = challenge.getCaptchaId();
					Request receiveRequest = buildReceiveCaptchaRequest(captchaId);

					try (Response sendResponse = captchaClient.newCall(receiveRequest).execute()) {
						String body = sendResponse.body().string();

						if (body.startsWith("ERROR")) {
							if (body.contains("NOT_DECODED")) {
								// Captcha not ready, should wait
								if (getMaxWait() > 0 && LocalDateTime.now().isAfter(challenge.getSentTime().plusSeconds(getMaxWait()))) {
									logger.error("This captcha has been waiting too long, drop it. Increase `maxWait` if that happens too often");
									challenges.remove(challenge);
								} else {
									// Keep the challenge in queue
									challenge.setNbPolls(challenge.getNbPolls() + 1);
								}
							} else if (body.contains("IMAGE_TIMED_OUT")) {
								logger.warn("ImageTypers IMAGE_TIMED_OUT. Try again");
								challenges.remove(challenge);
							} else if (body.contains("INVALID_CAPTCHA_ID")) {
								logger.error("ImageTypers reported captcha ID as invdalid : " + captchaId);
								challenges.remove(challenge);
							} else {
								logger.error("Unknown ImageTypers Error : " + body);
								challenges.remove(challenge);
							}
						} else {
							String response = body;
							logger.info("Captcha response given in {}s", ChronoUnit.SECONDS.between(challenge.getSentTime(), LocalDateTime.now()));
							queue.addCaptcha(response);
							challenges.remove(challenge);
						}
					} catch (IOException e) {
						logger.error("ImageTypers Error : " + e.getMessage(), e);
						logger.debug("ImageTypers Error details", e);
						challenges.remove(challenge);
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

						if (body.contains("ERROR")) {
							if (body.contains("INSUFFICIENT_BALANCE")) {
								if (manageInsufficientBalanceStop()) {
									break;
								}
							} else {
								logger.error("Error while calling IN ImageTypers : {}", body);
							}
						} else {
							String captchaId = body;
							logger.info("Requested new Captcha, id : {}", captchaId);
							challenges.add(new ImageTypersRequest(captchaId));
						}
					} catch (Exception e) {
						if (INSUFFICIENT_BALANCE.equals(e.getMessage())) {
							if (manageInsufficientBalanceStop()) {
								break;
							}
						} else {
							logger.error("Error while calling IN ImageTypers : {}", e.getMessage());
						}
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
	 * Manage insufficient Balance
	 * 
	 * @return true if loop should stop
	 */
	private boolean manageInsufficientBalanceStop() {
		logger.error("Insufficient balance");
		if (stopOnInsufficientBalance) {
			logger.error("STOP");
			this.runFlag = false;
			return true;
		} else {
			logger.error("WAIT");
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e1) {
				// Interrupted
			}
		}
		return false;
	}

	/**
	 * Get Current Balance. Should be called at least once before use to check for valid key.
	 * 
	 * @return current balance in USD
	 * @throws TechnicalException
	 */
	public double getBalance() throws ImageTypersConfigurationException {
		try {
			Request sendRequest = buildBalanceCheckequestGet();
			Response sendResponse = captchaClient.newCall(sendRequest).execute();

			String body = sendResponse.body().string();
			if (body.contains("ERROR")) {
				if (body.contains("AUTHENTICATION_FAILED")) {
					throw new ImageTypersConfigurationException("Authentication failed, captcha key might be bad");
				} else {
					throw new ImageTypersConfigurationException(body);
				}
			}

			return Double.valueOf(body.replaceAll("\\$", ""));
		} catch (IOException e) {
			throw new ImageTypersConfigurationException("Error getting account balance", e);
		}
	}

	/**
	 * Send Captcha Request
	 * 
	 * @return
	 */
	private Request buildSendCaptchaRequest() {
		HttpUrl url = HttpUrl.parse(RECAPTCHA_SUBMIT_ENDPOINT).newBuilder()
				.addQueryParameter("action", "UPLOADCAPTCHA")
				.addQueryParameter("googlekey", googleSiteKey)
				.addQueryParameter("pageurl", PAGE_URL)
				.addQueryParameter("token", apiKey)
				.addQueryParameter("refid", softId)
				.addQueryParameter("affiliateid", softId)
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
		HttpUrl url = HttpUrl.parse(RECAPTCHA_RETRIEVE_ENDPOINT).newBuilder()
				.addQueryParameter("action", "GETTEXT")
				.addQueryParameter("captchaid", catpchaId)
				.addQueryParameter("ids", catpchaId)
				.addQueryParameter("token", apiKey)
				.addQueryParameter("refid", softId)
				.addQueryParameter("affiliateid", softId)
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
	private Request buildBalanceCheckequestGet() {
		HttpUrl url = HttpUrl.parse(BALANCE_ENDPOINT).newBuilder()
				.addQueryParameter("action", "REQUESTBALANCE")
				.addQueryParameter("token", apiKey)
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
	private Request buildBalanceCheckequestPost() {
		HttpUrl url = HttpUrl.parse(BALANCE_ENDPOINT).newBuilder()
				.build();

		FormBody body = new FormBody.Builder()
				.addEncoded("action", "REQUESTBALANCE")
				.addEncoded("token", apiKey)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		return request;
	}

}
