package com.kinancity.core.model;

import java.util.ArrayList;
import java.util.List;

import com.kinancity.api.model.AccountData;
import com.kinancity.core.status.CreationStatus;

import lombok.Data;

/**
 * Process of creating an account
 * 
 * @author drallieiv
 *
 */
@Data
public class AccountCreation {

	/**
	 * Status of the account creation
	 */
	private CreationStatus status = CreationStatus.NEW;

	/**
	 * List of failure reasons
	 */
	private List<CreationFailure> failures = new ArrayList<>();

	/**
	 * Information about the account itself
	 */
	private AccountData accountData;

	public AccountCreation(AccountData accountData) {
		this.accountData = accountData;
	}

}
