package com.kinancity.mail.tester;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.mail.MailConstants;

import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ThrottleTester {

	private static final String URL = "https://club.pokemon.com/us/pokemon-trainer-club/activated/";

	private Logger logger = LoggerFactory.getLogger(getClass());

	// Start at 5s delay between each request
	@Setter
	private int delay = 5000;

	private float delayRamp = .9f;

	@Setter
	private int rampCount = 20;

	@Setter
	private int errorDelay = 1000;

	@Setter
	private int pauseAfterNRequests = 6;

	@Setter
	private int delayAfterNRequests = 45000;

	private boolean stop = false;

	private Request request;

	private boolean hasFailed = false;

	public void start() {

		setup();

		logger.info("Start Testing Throttle");
		logValues();

		int chainSuccess = 0;
		int chainError = 0;

		while (!stop) {
			if (!hasFailed) {
				// Make calls until failure
				if (checkOK()) {
					chainSuccess++;
					logger.debug("{} success in a row", chainSuccess);
					if (chainSuccess >= rampCount) {
						logger.info("All {} requests at {}s delay OK", rampCount, asString(delay));
						delay = Math.round(delay * delayRamp);
						chainSuccess = 0;
						logValues();
					}

					if (pauseAfterNRequests != 0 && chainSuccess % pauseAfterNRequests == 0) {
						logger.info("Group of {} requests done, wait for {}s", pauseAfterNRequests, asString(delayAfterNRequests));
						pause(delayAfterNRequests);
					} else {
						pause(delay);
					}
				} else {
					hasFailed = true;
					logger.info("Failed after {} success at {}s delay", chainSuccess, asString(delay));
					logger.info("Start checking for release every {}s", asString(errorDelay));
					pause(errorDelay);
				}
			} else {
				if (!checkOK()) {
					// Still blocked
					chainError++;
					logger.debug("{} softban in a row", chainError);
					pause(errorDelay);
				} else {
					logger.info("softban released after {} calls with {}s delay", chainError, asString(errorDelay));
					stop = true;
				}
			}
		}
	}

	public String asString(int ms) {
		return String.format("%.02f", ms / 1000f);
	}

	public void pause(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private boolean checkOK() {
		try {
			Response response = newClient().newCall(request).execute();
			return response.isSuccessful();
		} catch (IOException e) {
			logger.error("ERROR : {}", e.getMessage());
			return false;
		}
	}

	private void setup() {
		request = new Request.Builder()
				.header(MailConstants.HEADER_USER_AGENT, MailConstants.CHROME_USER_AGENT)
				.url(URL + randomHash())
				.build();
	}

	private OkHttpClient newClient() {
		return new OkHttpClient.Builder().connectTimeout(1, TimeUnit.MINUTES).readTimeout(1, TimeUnit.MINUTES).build();
	}

	private String randomHash() {
		return RandomStringUtils.randomAlphanumeric(33);
	}

	private void logValues() {
		logger.info("Testing {} requests with delay {}s, and errorDelay {}s and {}s pause every {} requests", this.rampCount, asString(delay), asString(errorDelay), asString(delayAfterNRequests), this.pauseAfterNRequests);
	}
}
