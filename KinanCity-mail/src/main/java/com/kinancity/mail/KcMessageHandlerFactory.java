package com.kinancity.mail;

import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;

import com.kinancity.mail.activator.LinkActivator;

import lombok.Setter;

/**
 * Message Handler Factory
 * 
 * @author drallieiv
 *
 */
public class KcMessageHandlerFactory implements MessageHandlerFactory {

	private LinkActivator activator;

	@Setter
	private boolean acceptAllFrom = false;

	public KcMessageHandlerFactory(LinkActivator activator) {
		this.activator = activator;
	}

	@Override
	public MessageHandler create(MessageContext ctx) {
		KcMessageHandler handler = new KcMessageHandler(activator);
		handler.setAcceptAllFrom(acceptAllFrom);
		return handler;
	}

}
