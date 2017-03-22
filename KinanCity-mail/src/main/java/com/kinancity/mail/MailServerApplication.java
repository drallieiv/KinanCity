package com.kinancity.mail;

import org.subethamail.wiser.Wiser;

public class MailServerApplication {

	public static void main(String[] args) {

		LinkActivator activator = new LinkActivator();
		
		// Start Wiser server
		Wiser wiser = new Wiser();
		wiser.setPort(25);
		wiser.setHostname("localhost");

		wiser.getServer().setMessageHandlerFactory(new KcMessageHandlerFactory(activator));
		wiser.start();
	}

}
