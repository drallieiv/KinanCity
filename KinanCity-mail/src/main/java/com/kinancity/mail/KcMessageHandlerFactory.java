package com.kinancity.mail;

import com.kinancity.mail.mailchanger.EmailChanger;
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

	private EmailChanger emailChanger;

	@Setter
	private boolean acceptAllFrom = false;

	public KcMessageHandlerFactory(LinkActivator activator, EmailChanger emailChanger) {
		this.activator = activator;
		this.emailChanger = emailChanger;
	}

	@Override
	public MessageHandler create(MessageContext ctx) {
		KcMessageHandler handler = new KcMessageHandler(activator, emailChanger);
		handler.setAcceptAllFrom(acceptAllFrom);
		return handler;
	}

}
