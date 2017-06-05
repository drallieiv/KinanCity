package com.kinancity.api.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * Information about a PTC account
 *
 * @author drallieiv
 *
 */
@Getter
public class AccountData implements Cloneable {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Setter
	private String username;

	@Setter
	private String email;

	@Setter
	private String password;

	// String Date of birth as YYYY-MM-DD
	private String dob;

	private String country;

	public static final String DEFAULT_COUNTRY = "US";

	private SimpleDateFormat dobFormat = new SimpleDateFormat("yyyy-MM-dd");

	public AccountData() {
		this(null, null, null, null, null);
	}

	public AccountData(String username, String email, String password) {
		this(username, email, password, null, null);
	}

	public AccountData(String username, String email, String password, String dob, String country) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.dob = (dob != null) ? dob : randomAdultDateOfBirth();
		this.country = (country != null) ? country : DEFAULT_COUNTRY;
	}

	public void setDob(String dob) {
		try {
			if (dobFormat.parse(dob) != null) {
				this.dob = dob;
			}
		} catch (ParseException e) {
			logger.error("Invalid date {}, keep default date", dob);
		}

	}

	public void setCountry(String country) {
		// TODO we should check that this country is supported by PTC
		this.country = country;
	}

	public AccountData clone() {
		return new AccountData(username, email, password, dob, country);
	}

	public String toCsv() {
		return username + ";" + password + ";" + email + ";" + dob + ";" + country;
	}

	@Override
	public String toString() {
		return "AccountData [" + (username != null ? "username=" + username + ", " : "") + (email != null ? "email=" + email + ", " : "") + (password != null ? "password=" + password + ", " : "") + (dob != null ? "dob=" + dob + ", " : "") + (country != null ? "country=" + country : "") + "]";
	}

	public static String randomAdultDateOfBirth() {
		return randomDateOfBirth(18, 80);
	}

	public static String randomDateOfBirth(int minAge, int maxAge) {
		int randomDays = RandomUtils.nextInt(maxAge * 365);
		LocalDateTime dobDate = LocalDateTime.now().minusYears(minAge).minusDays(randomDays);
		return dobDate.format(DateTimeFormatter.ISO_DATE);
	}

}
