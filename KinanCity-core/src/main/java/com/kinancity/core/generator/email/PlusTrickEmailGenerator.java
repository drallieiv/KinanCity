package com.kinancity.core.generator.email;

import org.apache.commons.lang.StringUtils;

import com.kinancity.core.generator.EmailGenerator;

import lombok.Setter;

/**
 * Generate an email using the Plus Trick <br>
 * <br>
 * - "address@domain.com" => address+username@domain.com <br>
 * - "@domain.com" => username@domain.com <br>
 * 
 * @author drallieiv
 *
 */
public class PlusTrickEmailGenerator implements EmailGenerator {

	@Setter
	private String baseEmail;

	// If false, replace everything before the @
	// If true, just insert with a joiner
	private boolean useJoiner = true;

	private boolean initDone = false;

	private String joiner = "+";

	public PlusTrickEmailGenerator(String baseEmail) {
		this.baseEmail = baseEmail;
		init();
	}

	public void init() {
		if (baseEmail != null && !StringUtils.contains(baseEmail, "@")) {
			throw new IllegalArgumentException("invalid or missing base email");
		}

		if (baseEmail.startsWith("@")) {
			useJoiner = false;
		}

		initDone = true;
	}

	@Override
	public String generateEmail(String username) {
		if (!initDone) {
			init();
		}
		return baseEmail.replaceFirst("@", (useJoiner ? joiner : "") + username + "@");
	}

}
