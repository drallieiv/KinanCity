package com.kinancity.core.generator.account;

import com.kinancity.api.model.AccountData;
import com.kinancity.core.generator.AccountGenerator;

import lombok.Getter;

/**
 * Generator that will only provide 1 account
 * 
 * @author drallieiv
 *
 */
@Getter
public class SingleAccountGenerator implements AccountGenerator {

	// The account data to give
	private AccountData data;

	// Once flag
	private boolean read = false;

	public SingleAccountGenerator(AccountData data) {
		super();
		this.data = data;
	}

	@Override
	public boolean hasNext() {
		return !read;
	}

	@Override
	public AccountData next() {
		if (read) {
			return null;
		}
		read = true;
		return data;
	}

}
