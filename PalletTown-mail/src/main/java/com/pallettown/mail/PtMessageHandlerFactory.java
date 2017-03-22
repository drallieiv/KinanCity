package com.pallettown.mail;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;

/**
 * Message Handler Factory
 * 
 * @author drallieiv
 *
 */
public class PtMessageHandlerFactory implements MessageHandlerFactory {

	private PtLinkActivator activator;
	
	public PtMessageHandlerFactory(PtLinkActivator activator) {
		this.activator = activator;
	}

	@Override
	public MessageHandler create(MessageContext ctx) {
		return new PtMessageHandler(activator);
	}

}
