package com.kinancity.mail;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;

/**
 * Message Handler Factory
 * 
 * @author drallieiv
 *
 */
public class KcMessageHandlerFactory implements MessageHandlerFactory {

	private LinkActivator activator;

	public KcMessageHandlerFactory(LinkActivator activator) {
		this.activator = activator;
	}

	@Override
	public MessageHandler create(MessageContext ctx) {
		return new KcMessageHandler(activator);
	}

}
