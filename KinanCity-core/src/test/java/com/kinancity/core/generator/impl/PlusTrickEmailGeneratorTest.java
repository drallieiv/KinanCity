package com.kinancity.core.generator.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.kinancity.core.generator.EmailGenerator;
import com.kinancity.core.generator.email.PlusTrickEmailGenerator;

public class PlusTrickEmailGeneratorTest {

	/**
	 * If you have a mail that support + trick
	 */
	@Test
	public void plusTest() {
		// Given
		String baseEmail = "myname@domain.fr";
		String username = "username";

		// When
		EmailGenerator generator = new PlusTrickEmailGenerator(baseEmail);
		String email = generator.generateEmail(username);

		// Then
		assertThat(email).isNotNull();
		assertThat(email).isEqualTo("myname+username@domain.fr");
	}
	
	/**
	 * if you own you domain and setup a catch-all
	 */
	@Test
	public void customDomainTest() {
		// Given
		String baseEmail = "@mydomain.fr";
		String username = "username";

		// When
		EmailGenerator generator = new PlusTrickEmailGenerator(baseEmail);
		String email = generator.generateEmail(username);

		// Then
		assertThat(email).isNotNull();
		assertThat(email).isEqualTo("username@mydomain.fr");
	}

}
