package com.kinancity.mail;

import org.subethamail.wiser.Wiser;

import com.kinancity.mail.activator.DirectLinkActivator;
import com.kinancity.mail.activator.LinkActivator;
import com.kinancity.mail.activator.ToFileLinkActivator;
import com.kinancity.mail.tester.ThrottleTester;

public class MailServerApplication {

	public static void main(String[] args) {

		String mode = "mail";

		if (args.length > 0) {
			mode = args[0].toLowerCase();
		}

		if (mode.equals("test")) {
			System.out.println("Start in Tester Mode");

			ThrottleTester tester = new ThrottleTester();

			if (args.length > 1) {
				tester.setDelay(Integer.parseInt(args[1]));
			}

			if (args.length > 2) {
				tester.setErrorDelay(Integer.parseInt(args[2]));
			}
			
			if (args.length > 3) {
				tester.setPauseAfterNRequests(Integer.parseInt(args[3]));
			}
			
			if (args.length > 4) {
				tester.setDelayAfterNRequests(Integer.parseInt(args[4]));
			}

			tester.start();
		} else {
			LinkActivator activator = new DirectLinkActivator();
			if (mode.equals("log")) {
				System.out.println("Started in Log Mode");
				activator = new ToFileLinkActivator();
			}

			// Start Wiser server
			Wiser wiser = new Wiser();
			wiser.setPort(25);
			wiser.setHostname("localhost");

			wiser.getServer().setMessageHandlerFactory(new KcMessageHandlerFactory(activator));			
			wiser.start();
		}
	}

}
