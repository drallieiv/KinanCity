package com.kinancity.core.scheduling;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.model.AccountCreation;

public class AccountCreationQueue extends ArrayDeque<AccountCreation> {

	private static final long serialVersionUID = -9140318791450328632L;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public synchronized AccountCreation pop() {
		try {
			if (!this.isEmpty()) {
				return super.pop();
			}
			return null;
		} catch (NoSuchElementException e) {
			logger.debug("Last element was removed before we can get it");
			return null;
		}
	}

}
