package com.kinancity.core.generator.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.kinancity.api.model.AccountData;
import com.kinancity.core.generator.account.SequenceAccountGenerator;
import com.kinancity.core.generator.password.RandomPasswordGenerator;
import com.kinancity.core.generator.password.SinglePasswordGenerator;

public class SequenceAccountGeneratorTest {

	@Test
	public void sequenceTest() {
		// Given
		SequenceAccountGenerator generator = new SequenceAccountGenerator();
		generator.setBaseEmail("myname@domain.fr");
		generator.setUsernamePattern("pref*****suf");
		generator.setStartFrom(1234);
		generator.setNbAccounts(4);
		
		AccountData data;
		
		generator.setPasswordGenerator(new SinglePasswordGenerator("Test"));
		
		data = generator.next();
		assertThat(data).isNotNull();
		assertThat(data.getUsername()).isEqualTo("pref01234suf");
		assertThat(data.getPassword()).isEqualTo("Test");
		
		generator.setPasswordGenerator(new RandomPasswordGenerator(-1));
		
		data = generator.next();
		assertThat(data).isNotNull();
		assertThat(data.getUsername()).isEqualTo("pref01235suf");
		assertThat(data.getPassword().length()).isBetween(8, 50);
		
		generator.setPasswordGenerator(new RandomPasswordGenerator(5));
		
		data = generator.next();
		assertThat(data).isNotNull();
		assertThat(data.getUsername()).isEqualTo("pref01236suf");
		assertThat(data.getPassword().length()).isEqualTo(8);
				
		generator.setPasswordGenerator(new RandomPasswordGenerator(100));
		
		data = generator.next();
		assertThat(data).isNotNull();
		assertThat(data.getUsername()).isEqualTo("pref01237suf");
		assertThat(data.getPassword().length()).isEqualTo(50);
		
		data = generator.next();
		assertThat(data).isNull();
	}

}
