package com.kinancity.api.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
	private String dob = "1985-01-16";

	private String country = "US";

	private SimpleDateFormat dobFormat = new SimpleDateFormat("yyyy-MM-dd");

	public AccountData() {

	}

	public AccountData(String username, String email, String password) {
		this.username = username;
		this.email = email;
		this.password = password;
	}

	public AccountData(String username, String email, String password, String dob, String country) {
		this.username = username;
		this.email = email;
		this.password = password;
		this.dob = dob;
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

}
