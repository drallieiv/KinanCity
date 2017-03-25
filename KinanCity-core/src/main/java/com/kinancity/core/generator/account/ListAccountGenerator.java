package com.kinancity.core.generator.account;

import java.util.ArrayDeque;
import java.util.Collection;

import com.kinancity.api.model.AccountData;
import com.kinancity.core.generator.AccountGenerator;

/**
 * Generator that has been given a list of account and give them back one by one.
 * 
 * @author drallieiv
 *
 */
public class ListAccountGenerator implements AccountGenerator {

	private ArrayDeque<AccountData> accounts;

	public ListAccountGenerator() {
		accounts = new ArrayDeque<>();
	}

	public boolean addAll(Collection<AccountData> newAccounts) {
		return accounts.addAll(newAccounts);
	}

	public boolean add(AccountData newAccount) {
		return accounts.add(newAccount);
	}

	@Override
	public boolean hasNext() {
		return ! accounts.isEmpty();
	}

	@Override
	public AccountData next() {
		return accounts.pop();
	}

}
