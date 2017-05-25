package com.kinancity.mail.activator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fake Activator that just saves as file
 * @author drallieiv
 *
 */
public class ToFileLinkActivator implements LinkActivator{

	private Logger logger = LoggerFactory.getLogger("LINKS");
	
	@Override
	public boolean activateLink(String link) {
		logger.info("{};skipped",link);
		return true;
	}

}
