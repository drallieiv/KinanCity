package com.kinancity.core.generator.account;

import com.kinancity.api.model.AccountData;
import com.kinancity.core.generator.AccountGenerator;
import com.kinancity.core.generator.EmailGenerator;
import com.kinancity.core.generator.PasswordGenerator;
import com.kinancity.core.generator.UsernameGenerator;
import com.kinancity.core.generator.email.PlusTrickEmailGenerator;
import com.kinancity.core.generator.password.SinglePasswordGenerator;
import com.kinancity.core.generator.username.SequenceUsernameGenerator;

import lombok.Setter;

/**
 * Default Sequence Account Generator
 * 
 * @author drallieiv
 *
 */
public class SequenceAccountGenerator implements AccountGenerator {

	@Setter
	// Format with prefix and suffix. use * instead of the number sequence
	private String usernamePattern;

	@Setter
	private String staticPassword;

	@Setter
	private String baseEmail;

	@Setter
	// First value of sequence
	private int startFrom = 0;

	@Setter
	// How many accounts you want
	private int nbAccounts = 1;

	private int nbCreated = 0;

	@Setter
	private EmailGenerator emailGenerator;

	@Setter
	private UsernameGenerator usernameGenerator;

	@Setter
	private PasswordGenerator passwordGenerator;

	private boolean initDone = false;

	public void init() {

		emailGenerator = new PlusTrickEmailGenerator(baseEmail);

		SequenceUsernameGenerator seqUsernameGenerator = new SequenceUsernameGenerator(usernamePattern, startFrom);
		if (startFrom + nbAccounts > seqUsernameGenerator.getSequenceCount()) {
			throw new IllegalArgumentException("Sequence would overflow format, use more *");
		}
		usernameGenerator = seqUsernameGenerator;

		passwordGenerator = new SinglePasswordGenerator(staticPassword);

		initDone = true;
	}

	@Override
	public boolean hasNext() {
		return nbCreated < nbAccounts;
	}

	@Override
	public AccountData next() {
		if (!initDone) {
			init();
		}

		if (!hasNext()) {
			return null;
		} else {
			nbCreated++;
		}

		String username = usernameGenerator.generateUsername();
		String email = emailGenerator.generateEmail(username);
		String password = passwordGenerator.generatePassword();

		return new AccountData(username, email, password);
	}

}
