package com.kinancity.mail.activator;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.mail.Activation;
import com.kinancity.mail.FileLogger;
import com.kinancity.mail.MailConstants;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Class that will take care of following the activation link
 * 
 * @author drallieiv
 *
 */
public class DirectLinkActivator implements LinkActivator {
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String SUCCESS_MSG = "Thank you for signing up! Your account is now active.";
	private static final String ALREADY_DONE_MSG = "Your account has already been activated.";
	private static final String INVALID_TOKEN_MSG = "We cannot find an account matching the confirmation email.";
	private static final String THROTTLE_MSG = "403 Forbidden";

	private okhttp3.OkHttpClient client;

	public DirectLinkActivator() {
		new OkHttpClient.Builder()
		.readTimeout(10,TimeUnit.SECONDS)
		.retryOnConnectionFailure(false)
		.connectionPool(new ConnectionPool(0,0,TimeUnit.SECONDS))
		.build();
	}

	public static void main(String[] args) throws IOException {
		boolean stop = false;
		while (!stop) {
			LinkActivator activator = new DirectLinkActivator();
			activator.activateLink(new Activation(args[0], "test@mail.com"));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.kinancity.mail.activator.LinkActivator#activateLink(java.lang.String)
	 */
	public boolean activateLink(Activation link) {
		try {
			Request request = new Request.Builder()
					.header(MailConstants.HEADER_USER_AGENT, MailConstants.CHROME_USER_AGENT)
					.url(link.getLink())
					.build();
			Response response = client.newCall(request).execute();

			String strResponse = response.body().string();

			client.dispatcher().executorService().shutdown();
			client.connectionPool().evictAll();

			if (response.isSuccessful()) {
				if (strResponse.contains(SUCCESS_MSG)) {
					logger.info("Activation success : Your account is now active");
					FileLogger.logStatus(link, FileLogger.OK);
					return true;
				}

				logger.info("Activation success");
				return true;
			} else {
				if (strResponse.contains(ALREADY_DONE_MSG)) {
					logger.info("Activation already done");
					FileLogger.logStatus(link, FileLogger.DONE);
					return true;
				}

				if (strResponse.contains(INVALID_TOKEN_MSG)) {
					logger.error("Invalid Activation token");
					FileLogger.logStatus(link, FileLogger.BAD);
					return false;
				}

				if (response.code() == 503 && strResponse.contains(THROTTLE_MSG)) {
					logger.error("HTTP 503. Your validation request was throttled");
					FileLogger.logStatus(link, FileLogger.THROTTLED);
					return false;
				}

				logger.error("Unexpected Error : {}", strResponse);
				FileLogger.logStatus(link, FileLogger.ERROR);
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public void start() {
		// Nothing to do
	}

}
