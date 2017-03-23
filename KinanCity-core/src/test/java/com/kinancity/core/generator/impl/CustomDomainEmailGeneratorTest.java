package com.kinancity.core.generator.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CustomDomainEmailGeneratorTest {

	@Test
	public void sequenceTest() {
		// Given
		CustomDomainEmailGenerator generator = new CustomDomainEmailGenerator();
		String baseEmail = "myname@domain.fr";
		String username = "username";
		
		String email = generator.generateEmail(baseEmail, username);
		assertThat(email).isNotNull();
		assertThat(email).isEqualTo("username@domain.fr");
		
	}

}
