package com.kinancity.core.creation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.Configuration;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.csv.CsvHeaderConstants;
import com.kinancity.core.data.AccountData;
import com.kinancity.core.errors.AccountCreationException;

public class PtcAccountCreator {

	private static final String CSV_COMMENT_PREFIX = "#";

	private static final String CSV_SPLITTER = ";";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private Configuration config;
	
	private final ExecutorService pool;


	public PtcAccountCreator(Configuration config) {
		this.config = config;
		
		ThreadFactory threadFactory = new PtcAccountCreatorThreadFactory();
		pool = Executors.newFixedThreadPool(config.getNbThreads(), threadFactory);
	}

	// Create an account
	public PtcCreationResult createAccount(AccountData account) throws AccountCreationException {
		return (new PtcAccountCreationTask(account, config)).call();
	}

	/**
	 * Create accounts listed in a csv file
	 * 
	 * @param accountFileName
	 * @return 
	 */
	public PtcCreationSummary createAccounts(String accountFileName) {
		logger.info("Creating all accounts defined in file {}", accountFileName);

		File accountFile = new File(accountFileName);
		if (!accountFile.exists() || !accountFile.canRead()) {
			logger.error("Cannot open file {}. Abort", accountFileName);
		}

		try (Scanner scanner = new Scanner(accountFile)){

			List<AccountData> accountsToCreate = new ArrayList<>();
			List<String> headers = null;
			
			while (scanner.hasNext()) {
				String line = scanner.nextLine();

				if (line.startsWith(CSV_COMMENT_PREFIX)) {
					headers = Arrays.asList(line.replace(CSV_COMMENT_PREFIX, "").split(CSV_SPLITTER));

					if (!headers.containsAll(Arrays.asList(CsvHeaderConstants.USERNAME, CsvHeaderConstants.PASSWORD, CsvHeaderConstants.EMAIL))) {
						return new PtcCreationSummary("CSV file is missing either username, password or email fields. Aborted");
					}

				} else {
					if (headers == null) {
						return new PtcCreationSummary("CSV file is missing header line");
					}

					accountsToCreate.add(buildAccountDataFromCsv(line, headers));
				}
			}
			return createAccounts(accountsToCreate);

		} catch (FileNotFoundException e) {
			logger.error("Cannot open file {}", accountFileName, e);
			return new PtcCreationSummary("Cannot open file");
		} catch (AccountCreationException e) {
			logger.error("creation failed", e);
			return new PtcCreationSummary("Creation Failed");
		}
		
	}

	/**
	 * Create multiple accounts as once
	 * 
	 * @param accountsToCreate
	 * @return 
	 * @throws AccountCreationException
	 */
	private PtcCreationSummary createAccounts(List<AccountData> accountsToCreate) throws AccountCreationException {
		logger.info("Start creating {} account in batch loaded from csv");

		// add all accounts to pool
		List<Future<PtcCreationResult>> futures = new ArrayList<>();
		for (AccountData accountData : accountsToCreate) {
			futures.add(pool.submit(new PtcAccountCreationTask(accountData, config)));
		}
		
		PtcCreationSummary summary = new PtcCreationSummary();
		
		for(Future<PtcCreationResult> future : futures){
		    try {
		    	summary.add(future.get());
			} catch (InterruptedException | ExecutionException e) {
				throw new AccountCreationException(e);
			}
		}
		
		return summary;
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
