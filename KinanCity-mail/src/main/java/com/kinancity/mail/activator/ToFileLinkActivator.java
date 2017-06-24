package com.kinancity.mail.activator;

import com.kinancity.mail.Activation;
import com.kinancity.mail.FileLogger;

/**
 * Fake Activator that just saves as file
 * 
 * @author drallieiv
 *
 */
public class ToFileLinkActivator implements LinkActivator {

	@Override
	public boolean activateLink(Activation link) {
		FileLogger.logStatus(link, FileLogger.SKIPPED);
		return true;
	}

}
