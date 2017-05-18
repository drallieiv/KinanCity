package com.kinancity.mail;

import org.subethamail.wiser.Wiser;

import com.kinancity.mail.activator.DirectLinkActivator;
import com.kinancity.mail.activator.LinkActivator;

public class MailServerApplication {

	public static void main(String[] args) {

		LinkActivator activator = new DirectLinkActivator();
		
		// Start Wiser server
		Wiser wiser = new Wiser();
		wiser.setPort(25);
		wiser.setHostname("localhost");

		wiser.getServer().setMessageHandlerFactory(new KcMessageHandlerFactory(activator));
		wiser.start();
	}

}
