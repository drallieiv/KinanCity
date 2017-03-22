package com.pallettown.mail;

import org.subethamail.wiser.Wiser;

public class PtMailServerApplication {

	public static void main(String[] args) {

		PtLinkActivator activator = new PtLinkActivator();
		
		// Start Wiser server
		Wiser wiser = new Wiser();
		wiser.setPort(25);
		wiser.setHostname("localhost");

		wiser.getServer().setMessageHandlerFactory(new PtMessageHandlerFactory(activator));
		wiser.start();
	}

}
