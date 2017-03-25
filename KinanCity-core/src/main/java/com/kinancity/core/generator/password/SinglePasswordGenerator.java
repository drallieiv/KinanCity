package com.kinancity.core.generator.password;

import com.kinancity.core.generator.PasswordGenerator;

/**
 * Simple Password generator that always give the same
 * @author drallieiv
 *
 */
public class SinglePasswordGenerator implements PasswordGenerator {

	private String password;

	public SinglePasswordGenerator(String password) {
		super();
		this.password = password;
	}

	@Override
	public String generatePassword() {
		return password;
	}

}
