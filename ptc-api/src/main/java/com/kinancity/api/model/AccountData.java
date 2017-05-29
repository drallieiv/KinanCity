package com.kinancity.api.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Random;

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

	private String country = "US";

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
		setDob(dob != null ? dob : chooseRandomDateOfBirth());
		this.country = country;
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

	private String chooseRandomDateOfBirth() {
		Random generator = new Random();
		// year: 1970 to 2000
		int y = generator.nextInt(30) + 1970;
		// month: 1 to 12
		int m = generator.nextInt(12) + 1;
		// day: depending on month
		int maxDays;
		switch (m) {
			case 2:
				maxDays = 28;
				break;
			case 4:
			case 6:
			case 9:
			case 11:
				maxDays = 30;
				break;
			default:
				maxDays = 31;
		}
		int d = generator.nextInt(maxDays) + 1;
		return String.format("%04d-%02d-%02d", y, m, d);
	}

}
