package com.kinancity.mail.activator;

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
public class DirectLinkActivator implements LinkActivator {

	private Logger fileLogger = LoggerFactory.getLogger("LINKS");
	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String SUCCESS_MSG = "Thank you for signing up! Your account is now active.";
	private static final String ALREADY_DONE_MSG = "Your account has already been activated.";
	private static final String INVALID_TOKEN_MSG = "We cannot find an account matching the confirmation email.";
	private static final String THROTTLE_MSG = "403 Forbidden";

	private okhttp3.OkHttpClient client;

	public DirectLinkActivator() {
		client = new OkHttpClient.Builder().build();
	}

	public static void main(String[] args) throws IOException {
		boolean stop = false;
		while (! stop){
			LinkActivator activator = new DirectLinkActivator();
			activator.activateLink(args[0]);
		}
	}

	/* (non-Javadoc)
	 * @see com.kinancity.mail.activator.LinkActivator#activateLink(java.lang.String)
	 */
	public boolean activateLink(String link) {
		try {
			Request request = new Request.Builder().url(link).build();
			Response response = client.newCall(request).execute();

			String strResponse = response.body().string();

			if(response.isSuccessful()){
				if (strResponse.contains(SUCCESS_MSG)) {
					logger.info("Activation success : Your account is now active");
					fileLogger.info("{};OK",link);
					return true;
				}
				
				logger.info("Activation success");
				return true;
			}else{ 
				if (strResponse.contains(ALREADY_DONE_MSG)) {
					logger.info("Activation already done");
					fileLogger.info("{};DONE",link);
					return true;
				}

				if (strResponse.contains(INVALID_TOKEN_MSG)) {
					logger.error("Invalid Activation token");
					fileLogger.info("{};BAD",link);
					return false;
				}
				
				if (response.code() == 503 && strResponse.contains(THROTTLE_MSG)) {
					logger.error("HTTP 503. Your validation request was throttled");
					fileLogger.info("{};THROTTLED",link);
					return false;
				}
				
				logger.error("Unexpected Error : {}", strResponse);
				fileLogger.info("{};ERROR",link);
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}
}
