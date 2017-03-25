package com.kinancity.core.generator.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.kinancity.core.data.AccountData;

public class SequenceAccountGeneratorTest {

	@Test
	public void sequenceTest() {
		// Given
		SequenceAccountGenerator generator = new SequenceAccountGenerator();
		generator.setBaseEmail("myname@domain.fr");
		generator.setUsernamePattern("pref*****suf");
		generator.setStartFrom(1234);
		generator.setNbAccounts(3);
		
		AccountData data;
		
		data = generator.nextAccountData();
		assertThat(data).isNotNull();
		assertThat(data.username).isEqualTo("pref01234suf");
		
		data = generator.nextAccountData();
		assertThat(data).isNotNull();
		assertThat(data.username).isEqualTo("pref01235suf");
		
		data = generator.nextAccountData();
		assertThat(data).isNotNull();
		assertThat(data.username).isEqualTo("pref01236suf");
		
		data = generator.nextAccountData();
		assertThat(data).isNull();
	}

}
