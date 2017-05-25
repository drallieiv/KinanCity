package com.kinancity.mail;

import org.subethamail.wiser.Wiser;

import com.kinancity.mail.activator.DirectLinkActivator;
import com.kinancity.mail.activator.LinkActivator;
import com.kinancity.mail.tester.ThrottleTester;

public class MailServerApplication {

	public static void main(String[] args) {
		
		String mode = "MAIL";
		
		if (args.length >= 0) {
			mode = args[0].toLowerCase();
		}

		if (mode.equals("test")) {
			System.out.println("Start in Tester Mode");
			
			ThrottleTester tester = new ThrottleTester();
			
			if(args.length > 1){
				tester.setDelay(Integer.parseInt(args[1]));
			}
			
			if(args.length > 2){
				tester.setErrorDelay(Integer.parseInt(args[2]));
			}
						
			tester.start();
		} else {
			LinkActivator activator = new DirectLinkActivator();

			// Start Wiser server
			Wiser wiser = new Wiser();
			wiser.setPort(25);
			wiser.setHostname("localhost");

			wiser.getServer().setMessageHandlerFactory(new KcMessageHandlerFactory(activator));
			wiser.start();
		}
	}

}
