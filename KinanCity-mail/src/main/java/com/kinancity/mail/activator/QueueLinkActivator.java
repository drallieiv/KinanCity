package com.kinancity.mail.activator;

import java.io.IOException;
import java.util.ArrayDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.mail.Activation;
import com.kinancity.mail.FileLogger;
import com.kinancity.mail.MailConstants;
import com.kinancity.mail.proxy.HttpProxy;

import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Class that will take care of following the activation link
 * 
 * @author drallieiv
 *
 */
public class QueueLinkActivator implements LinkActivator, Runnable {

	private static final int THROTTLE_PAUSE = 60000;
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String SUCCESS_MSG = "Thank you for signing up! Your account is now active.";
	private static final String ALREADY_DONE_MSG = "Your account has already been activated.";
	private static final String INVALID_TOKEN_MSG = "We cannot find an account matching the confirmation email.";
	private static final String THROTTLE_MSG = "403 Forbidden";

	private okhttp3.OkHttpClient client;

	private ArrayDeque<Activation> linkQueue;

	@Setter
	private boolean stop = false;

	@Setter
	private HttpProxy httpProxy;

	public QueueLinkActivator() {
		if (httpProxy != null) {
			client = httpProxy.getClient();
		} else {
			client = new OkHttpClient.Builder().build();
		}

		linkQueue = new ArrayDeque<>();

		Thread process = new Thread(this);
		process.start();
	}

	public boolean activateLink(Activation link) {
		linkQueue.add(link);
		return true;
	}

	public boolean realActivateLink(Activation link) {
		try {

			logger.info("Start actvation of link : {}", link);

			Request request = new Request.Builder()
					.header(MailConstants.HEADER_USER_AGENT, MailConstants.CHROME_USER_AGENT)
					.url(link.getLink())
					.build();

			boolean isFinal = false;
			boolean success = true;

			while (!isFinal) {
				Response response = client.newCall(request).execute();
				String strResponse = response.body().string();

				// By default, stop
				isFinal = true;

				if (response.isSuccessful()) {
					if (strResponse.contains(SUCCESS_MSG)) {
						logger.info("Activation success : Your account is now active");
						FileLogger.logStatus(link, FileLogger.OK);
					} else if (strResponse.contains(ALREADY_DONE_MSG)) {
						logger.info("Activation success : Activation already done");
						FileLogger.logStatus(link, FileLogger.DONE);
					} else if (strResponse.contains(INVALID_TOKEN_MSG)) {
						logger.error("Invalid Activation token");
						FileLogger.logStatus(link, FileLogger.BAD);
						success = false;
					} else {
						logger.warn("OK response but missing confirmation.");
						logger.debug("Body : \n {}", strResponse);
					}
				} else {
					if (response.code() == 503 && strResponse.contains(THROTTLE_MSG)) {
						logger.warn("HTTP 503. Your validation request was throttled, wait 60s");
						isFinal = false;
						throttlePause();
					} else {
						logger.error("Unexpected Error {} : {}", response.code(), strResponse);
						FileLogger.logStatus(link, FileLogger.ERROR);
						success = false;
					}

				}
			}

			return success;

		} catch (IOException e) {
			logger.error("IOException {}", e.getMessage());
			FileLogger.logStatus(link, FileLogger.ERROR);
			return false;
		}
	}

	public void throttlePause() {
		int split = 20;

		for (int i = 0; i < split; i++) {
			try {
				if (split > 0) {
					logger.info("...");
				}
				Thread.sleep(THROTTLE_PAUSE / split);
			} catch (InterruptedException e) {
				// Interrupted
				logger.warn("stoppped");
			}
		}
	}

	@Override
	public void run() {
		while (!stop || !linkQueue.isEmpty()) {
			if (linkQueue.isEmpty()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// Interrupted
					logger.warn("stoppped");
				}
			} else {
				Activation firstLink = linkQueue.pop();
				if (firstLink != null) {
					realActivateLink(firstLink);
				}
				logger.info("{} link to activate remaining", linkQueue.size());
			}
		}
	}
}
