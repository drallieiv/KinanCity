package com.pallettown.mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;

/**
 * Message Handler
 * 
 * @author drallieiv
 *
 */
public class PtMessageHandler implements MessageHandler {

	private static final String POKEMON_DOMAIN = "@pokemon.com";
	private static final String ACTIVATION_EXP = "https://club.pokemon.com/us/pokemon-trainer-club/activated/.*";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String recipient;
	private String from;
	
	private PtLinkActivator activator;

	/**
	 * Construct with a given Link Activator
	 * @param activator
	 */
	public PtMessageHandler(PtLinkActivator activator) {
		this.activator = activator;
	}

	@Override
	public void from(String from) throws RejectException {
		this.from = from;
	}

	@Override
	public void recipient(String recipient) throws RejectException {
		this.recipient = recipient;
	}

	/**
	 * Parse email, exctact activation link and call Link Activator
	 */
	@Override
	public void data(InputStream data) throws RejectException, TooMuchDataException, IOException {

		logger.debug("Received email from {} to {}", from, recipient);

		// Only accept pokemon mails
		if (from.endsWith(POKEMON_DOMAIN)) {
			try {
				Session session = Session.getDefaultInstance(new Properties());
				MimeMessage message = new MimeMessage(session, data);

				MimeMultipart mpart = (MimeMultipart) message.getContent();

				BodyPart part = mpart.getBodyPart(0);
				String content = (String) part.getContent();

				Pattern p = Pattern.compile(ACTIVATION_EXP);
				Matcher m = p.matcher(content);
				if (m.find()) {
					String activationLink = m.group(0);
					logger.info("Activation link found  for email {} : [{}]", recipient, activationLink);
					
					if(activator.activateLink(activationLink)){
						logger.info("Account activation for email {} : success", recipient);
					}else{
						logger.error("Account activation for email {} : failed", recipient);
					}
					
				} else {
					logger.error("No activation link found");
				}

			} catch (MessagingException e) {
				logger.error("Failed parsing Mime Message");
			}
		}

	}

	@Override
	public void done() {

	}

}
