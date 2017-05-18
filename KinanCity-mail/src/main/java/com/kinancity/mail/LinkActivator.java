package com.kinancity.mail;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Class that will take care of following the activation link
 * 
 * @author drallieiv
 *
 */
public class LinkActivator {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String SUCCESS_MSG = "Thank you for signing up! Your account is now active.";
	private static final String ALREADY_DONE_MSG = "Your account has already been activated.";
	private static final String INVALID_TOKEN_MSG = "We cannot find an account matching the confirmation email.";

	private okhttp3.OkHttpClient client;

	public LinkActivator() {
		client = new OkHttpClient.Builder().build();
	}

	public static void main(String[] args) throws IOException {
		LinkActivator activator = new LinkActivator();
		activator.activateLink(args[0]);
	}

	/**
	 * Activate link
	 * 
	 * @param link
	 *            activation url
	 * @return true if activation successfull
	 */
	public boolean activateLink(String link) {
		try {
			Request request = new Request.Builder().url(link).build();
			Response response = client.newCall(request).execute();

			String strResponse = response.body().string();

			if (strResponse.contains(SUCCESS_MSG)) {
				logger.info("Activation success");
				return true;
			}

			if (strResponse.contains(ALREADY_DONE_MSG)) {
				logger.info("Activation already done");
				return true;
			}

			if (strResponse.contains(INVALID_TOKEN_MSG)) {
				logger.error("Invalid Activation token");
				return false;
			}

			logger.error("Unexpected Error");

			return false;
		} catch (IOException e) {
			return false;
		}
	}
}
