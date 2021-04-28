package com.kinancity.mail;

import com.kinancity.mail.mailchanger.ToFileEmailChanger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLogger {

	public static final String OK = "OK";
	public static final String DONE = "DONE";
	public static final String BAD = "BAD";
	public static final String THROTTLED = "THROTTLED";
	public static final String ERROR = "ERROR";
	public static final String EXPIRED = "EXPIRED";
	
	public static final String SKIPPED = "SKIPPED";

	public static final String TYPE_MAILCHANGE = "MAILCHANGE";
	public static final String TYPE_ACTIVATION = "ACTIVATION";

	private static Logger LOGGER = LoggerFactory.getLogger("LINKS");

	public static void logStatus(Activation link, String status) {
		String type = (link instanceof EmailChangeRequest) ? TYPE_MAILCHANGE : TYPE_ACTIVATION;
		LOGGER.info("{};{};{};{}", type, link.getLink(), link.getEmail(), status);
	}

	public static Activation fromLog(String line) {
		String[] parts = line.split(";");
		if(parts[0].equals(TYPE_ACTIVATION)) {
			if (parts.length > 3) {
				return new Activation(parts[1], parts[2], parts[3]);
			} else {
				return new Activation(parts[1], null, parts[2]);
			}
		} else {
			if (parts.length > 3) {
				return new EmailChangeRequest(parts[1], parts[2], parts[3]);
			} else {
				return new EmailChangeRequest(parts[1], parts[2]);
			}
		}
	}
}
