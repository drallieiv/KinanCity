package com.kinancity.mail;

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

import com.kinancity.mail.mailchanger.EmailChanger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;

import com.kinancity.mail.activator.LinkActivator;

import lombok.Setter;

/**
 * Message Handler
 * 
 * @author drallieiv
 *
 */
public class KcMessageHandler implements MessageHandler {

	private static final String POKEMON_DOMAIN = "@pokemon.com";
	private static final String ACTIVATION_EXP = "https://club.pokemon.com/us/pokemon-trainer-club/activated/.*";
	private static final String EMAIL_CHANGE_EXP = "https://club.pokemon.com/us/pokemon-trainer-club/email-change-approval/[a-zA-Z0-9]+";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String recipient;
	private String from;

	private LinkActivator activator;

	private EmailChanger emailChanger;

	@Setter
	private boolean acceptAllFrom = false;

	private boolean handleActivation = true;

	private boolean handleEmailChange = true;

	@Setter
	private boolean logOtherMessages = true;

	/**
	 * Construct with a given Link Activator
	 * 
	 * @param activator
	 */
	public KcMessageHandler(LinkActivator activator, EmailChanger emailChanger) {
		this.activator = activator;
		this.emailChanger = emailChanger;

		handleActivation = activator != null;
		handleEmailChange = emailChanger != null;
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

		// Only accept pokemon mails
		if (from.endsWith(POKEMON_DOMAIN) || acceptAllFrom) {

			logger.debug("Received email from {} to {}", from, recipient);

			try {
				Session session = Session.getDefaultInstance(new Properties());
				MimeMessage message = new MimeMessage(session, data);

				MimeMultipart mpart = (MimeMultipart) message.getContent();

				BodyPart part = mpart.getBodyPart(0);
				String content = (String) part.getContent();

				boolean done = false;

				if(handleActivation) {
					if(searchForActivationLink(content)) {
						done = true;
					}
				}

				if(handleEmailChange && !done) {
					if(searchForEmailChangeRequestLink(content)) {
						done = true;
					}
				}

				if(logOtherMessages && !done) {
					logger.info("Other unknown mail received with content : {}", content);
				}

			} catch (MessagingException e) {
				logger.error("Failed parsing Mime Message");
			}
		} else {
			throw new RejectException(200, "Kinan : Email rejected");
		}

	}

	private boolean searchForActivationLink(String content) {
		Pattern p = Pattern.compile(ACTIVATION_EXP);
		Matcher m = p.matcher(content);
		if (m.find()) {
			String activationLink = m.group(0);
			logger.info("Activation link found  for email {} : [{}]", this.recipient, activationLink);

			// Link activation, may be sync or async
			activator.activateLink(new Activation(activationLink, this.recipient));
			return true;
		} else {
			logger.error("No activation link found");
			return false;
		}
	}

	private boolean searchForEmailChangeRequestLink(String content) {
		Pattern p = Pattern.compile(EMAIL_CHANGE_EXP);
		Matcher m = p.matcher(content);
		if (m.find()) {
			String emailChangeLink = m.group(0);
			logger.info("Email Change Request link found  for email {} : [{}]", this.recipient, emailChangeLink);

			// Email Change acceptation, may be sync or async
			emailChanger.acceptChange(new EmailChangeRequest(emailChangeLink, this.recipient));
			return true;
		} else {
			logger.error("No email change link found");
			return false;
		}
	}

	@Override
	public void done() {

	}

}
