package com.kinancity.mail.activator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.mail.Activation;
import com.kinancity.mail.FileLogger;

/**
 * Fake Activator that just saves as file
 * 
 * @author drallieiv
 *
 */
public class ToFileLinkActivator implements LinkActivator {

	private Logger logger = LoggerFactory.getLogger("LINKS");

	@Override
	public boolean activateLink(Activation link) {
		FileLogger.logStatus(link, FileLogger.SKIPPED);
		logger.info("{};skipped", link);
		return true;
	}

}
