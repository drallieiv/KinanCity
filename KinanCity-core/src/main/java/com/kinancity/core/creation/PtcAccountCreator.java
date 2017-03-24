package com.kinancity.core.creation;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
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
import java.util.concurrent.ThreadPoolExecutor;

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

	private List<Future<PtcCreationResult>> futures;

	public PtcAccountCreator(Configuration config) {
		this.config = config;

		ThreadFactory threadFactory = new PtcAccountCreatorThreadFactory();
		pool = Executors.newFixedThreadPool(config.getNbThreads(), threadFactory);
	}

	// Create an account
	public PtcCreationResult createAccount(AccountData account) throws AccountCreationException {
		return (new PtcAccountCreationTask(account, config, this)).call();
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

		try (Scanner scanner = new Scanner(accountFile)) {

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
	public PtcCreationSummary createAccounts(List<AccountData> accountsToCreate) throws AccountCreationException {

		LocalTime startTime = LocalTime.now();
		logger.info("Start creating {} account in batch loaded from csv");

		// add all accounts to pool
		futures = new ArrayList<>();

		long nbTotal = accountsToCreate.size();
		for (AccountData accountData : accountsToCreate) {
			this.schedule(new PtcAccountCreationTask(accountData, config, this));
		}

		try {
			// Show live progress
			long lastRunning = 0;
			long nbRunning = nbTotal;

			while (nbRunning > 0) {
				nbRunning = nbRunning();
				if (nbRunning != lastRunning) {
					logger.info("Batch creation progress : {}/{}", nbTotal - nbRunning, nbTotal);
					lastRunning = nbRunning;
				}
				if (nbRunning > 0) {
					Thread.sleep(10000);
				}
			}
		} catch (InterruptedException e) {
			throw new AccountCreationException(e);
		}

		logger.info("Start writing summary");

		// Generate final Summary
		PtcCreationSummary summary = new PtcCreationSummary();
		for (Future<PtcCreationResult> future : futures) {
			try {
				summary.add(future.get());
			} catch (InterruptedException | ExecutionException e) {
				summary.add(new PtcCreationResult(false, "failed", new AccountCreationException(e)));
			}
		}

		LocalTime endTime = LocalTime.now();
		summary.setDuration(startTime, endTime);

		logger.info("Batch summary : {}", summary);

		config.getResultLogWriter().close();

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

	public void reschedule(PtcAccountCreationTask origine) {
		this.schedule(origine.getRetry());
		logger.debug("There is now {} futures in queue with {} running", futures.size(), nbRunning());
	}
	
	public void schedule(PtcAccountCreationTask ptcAccountCreationTask) {
		futures.add(pool.submit(ptcAccountCreationTask));	
	}

	public long nbRunning() {
		return futures.stream().filter(future -> !future.isDone()).count();
	}



}
