package com.kinancity.mail.activator;

import com.kinancity.mail.Activation;

public interface LinkActivator {

	/**
	 * Activate link
	 * 
	 * @param link
	 *            activation data
	 * @return true if activation successfull
	 */
	boolean activateLink(Activation link);
	
	void start();

}