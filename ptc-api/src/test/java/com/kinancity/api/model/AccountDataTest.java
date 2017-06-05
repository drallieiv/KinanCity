package com.kinancity.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountDataTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void testAccountData() {
		AccountData account = new AccountData();
		assertThat(account.getDob()).isNotBlank();
	}

	@Test
	public void testChooseRandomDateOfBirth() {
		logger.debug("Checking 10 random adults DOB");
		for (int i = 0; i < 10; i++) {
			String rdob = AccountData.randomAdultDateOfBirth();
			logger.debug("Random DOB : {}", rdob);
			assertThat(rdob).isNotBlank();
			assertThat(rdob).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}");

			int year = Integer.parseInt((rdob.substring(0, 4)));
			int month = Integer.parseInt((rdob.substring(5, 7)));
			int day = Integer.parseInt((rdob.substring(8, 10)));

			assertThat(year).isGreaterThan(1900);
			assertThat(month).isLessThanOrEqualTo(12);
			assertThat(day).isLessThanOrEqualTo(31);

			int age = LocalDateTime.now().getYear() - year;
			assertThat(age).isGreaterThanOrEqualTo(18);
		}
	}

}
