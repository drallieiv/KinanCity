package com.kinancity.mail.activator;

public interface LinkActivator {

	/**
	 * Activate link
	 * 
	 * @param link
	 *            activation url
	 * @return true if activation successfull
	 */
	boolean activateLink(String link);

}