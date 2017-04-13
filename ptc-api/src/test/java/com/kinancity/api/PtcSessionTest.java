package com.kinancity.api;

import java.io.IOException;
import java.io.InputStream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import com.kinancity.api.errors.FatalException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.tech.AccountRateLimitExceededException;
import com.kinancity.api.model.AccountData;

public class PtcSessionTest {

	@Test(expected = AccountRateLimitExceededException.class)
	public void QuotaCheck() throws IOException, FatalException, TechnicalException {
		PtcSession session = new PtcSession(null);

		try (InputStream test1 = this.getClass().getResourceAsStream("/test1.html")) {
			AccountData account = new AccountData("username", "email", "password");
			Document doc = Jsoup.parse(test1, null, "");
			session.checkForErrors(account, doc);
		}

	}

}
