package com.kinancity.api.model;

import lombok.Data;

/**
 * Information about a PTC account
 * 
 * @author drallieiv
 *
 */
@Data
public class AccountData implements Cloneable {

	public String username;

	public String email;

	public String password;

	public AccountData() {

	}

	public AccountData(String username, String email, String password) {
		this.username = username;
		this.email = email;
		this.password = password;
	}

	public AccountData clone() {
		return new AccountData(username, email, password);
	}

	public String toCsv() {
		return username + ";" + password + ";" + email;
	}

}
