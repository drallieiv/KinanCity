package com.pallettown.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pallettown.core.captcha.CaptchaProvider;
import com.pallettown.core.csv.CsvHeaderConstants;
import com.pallettown.core.data.AccountData;
import com.pallettown.core.errors.AccountCreationException;

public class PTCAccountCreator {

	private static final String CSV_COMMENT_PREFIX = "#";

	private static final String CSV_SPLITTER = ";";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private PTCWebClient client;

	private CaptchaProvider captchaProvider;

	public PTCAccountCreator(CaptchaProvider captchaProvider) {
		client = new PTCWebClient();
		this.captchaProvider = captchaProvider;
	}

	// Create an account
	public void createAccount(AccountData account) throws AccountCreationException {

		logger.info("Create account with username {}", account);

		// 0. password and name check ?
		if (!client.validateAccount(account)) {
			logger.info("Invalid account will be skipped");
			return;
		}

		// 1. Grab a CRSF token
		String crsfToken = client.sendAgeCheckAndGrabCrsfToken();
		if (crsfToken == null) {
			throw new AccountCreationException("Could not grab CRSF token. pokemon-trainer-club website may be unavailable");
		}
		logger.debug("CRSF token found : {}", crsfToken);

		// 3. Captcha
		String captcha = captchaProvider.getCaptcha();

		// 4. Account Creation
		client.createAccount(account, crsfToken, captcha);

	}

	/**
	 * Create accounts listed in a csv file
	 * 
	 * @param accountFileName
	 */
	public void createAccounts(String accountFileName) {
		logger.info("Creating all accounts defined in file {}", accountFileName);

		File accountFile = new File(accountFileName);
		if (!accountFile.exists() || !accountFile.canRead()) {
			logger.error("Cannot open file {}. Abort", accountFileName);
		}

		try {

			List<AccountData> accountsToCreate = new ArrayList<>();

			List<String> headers = null;

			Scanner scanner = new Scanner(accountFile);
			while (scanner.hasNext()) {
				String line = scanner.nextLine();

				if (line.startsWith(CSV_COMMENT_PREFIX)) {
					headers = Arrays.asList(line.replace(CSV_COMMENT_PREFIX, "").split(CSV_SPLITTER));

					if (!headers.containsAll(Arrays.asList(CsvHeaderConstants.USERNAME, CsvHeaderConstants.PASSWORD, CsvHeaderConstants.EMAIL))) {
						logger.error("CSV file is missing either username, password or email fields. Abort");
						return;
					}

				} else {
					if (headers == null) {
						logger.error("CSV file is missing header line");
						return;
					}

					accountsToCreate.add(buildAccountDataFromCsv(line, headers));
				}
			}

			createAccounts(accountsToCreate);

		} catch (FileNotFoundException e) {
			logger.error("Cannot open file {}. Abort", accountFileName);
		} catch (AccountCreationException e) {
			logger.error("creation failed : {}", e.getMessage());
		}
	}

	/**
	 * Create multiple accounts as once
	 * 
	 * @param accountsToCreate
	 * @throws AccountCreationException
	 */
	private void createAccounts(List<AccountData> accountsToCreate) throws AccountCreationException {
		logger.info("Start creating {} account in batch loaded from csv");
		
		// TODO add a threaded pool
		for (AccountData accountData : accountsToCreate) {
			createAccount(accountData);
		}
	}

	/**
	 * Create an account from csv fields
	 * 
	 * @param line
	 * @param headers
	 * @return AccountData
	 */
	public AccountData buildAccountDataFromCsv(String line, List<String> headers) {
		// Parse csv into data map
		Map<String, String> fieldMap = new HashMap<>();
		List<String> fields = Arrays.asList(line.split(CSV_SPLITTER));
		for (int i = 0; i < Math.min(fields.size(), headers.size()); i++) {
			fieldMap.put(headers.get(i), fields.get(i));
		}
		return buildAccountDataFromMap(fieldMap);
	}

	/**
	 * Create an account given a set of fields
	 * 
	 * @param fieldMap
	 * @return AccountData
	 */
	public AccountData buildAccountDataFromMap(Map<String, String> fieldMap) {
		AccountData account = new AccountData();
		account.setUsername(fieldMap.get(CsvHeaderConstants.USERNAME));
		account.setPassword(fieldMap.get(CsvHeaderConstants.PASSWORD));
		account.setEmail(fieldMap.get(CsvHeaderConstants.EMAIL));
		return account;
	}

}
