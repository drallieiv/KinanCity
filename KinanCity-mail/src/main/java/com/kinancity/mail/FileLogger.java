package com.kinancity.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLogger {

	public static final String OK = "OK";
	public static final String DONE = "DONE";
	public static final String BAD = "BAD";
	public static final String THROTTLED = "THROTTLED";
	public static final String ERROR = "ERROR";
	
	public static final String SKIPPED = "SKIPPED";

	private static Logger LOGGER = LoggerFactory.getLogger("LINKS");

	public static void logStatus(Activation link, String status) {
		LOGGER.info("{};{};{}", link.getLink(), link.getEmail(), status);
	}

	public static Activation fromLog(String line) {
		String[] parts = line.split(";");
		if (parts.length > 2) {
			return new Activation(parts[0], parts[1], parts[2]);
		} else {
			return new Activation(parts[0], null, parts[1]);
		}
	}
}
