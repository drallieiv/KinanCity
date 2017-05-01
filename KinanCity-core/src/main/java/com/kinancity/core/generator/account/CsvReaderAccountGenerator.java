package com.kinancity.core.generator.account;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.model.AccountData;
import com.kinancity.core.generator.AccountGenerator;

/**
 * Generator that will only provide 1 account
 * 
 * @author drallieiv
 *
 */
public class CsvReaderAccountGenerator extends ListAccountGenerator implements AccountGenerator {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CSV_COMMENT_PREFIX = "#";

	private static final String CSV_SPLITTER = ";";

	public CsvReaderAccountGenerator(String accountFileName) {
		logger.info("Will load a list of accout to create from a csv file");

		File accountFile = new File(accountFileName);
		if (!accountFile.exists() || !accountFile.canRead()) {
			logger.error("Cannot open file {}. Abort", accountFileName);
		}

		try {
			loadFile(accountFile);
		} catch (FileNotFoundException e) {
			logger.error("Cannot open file {}. Abort", accountFileName);
		}
	}

	/**
	 * Read file and import all accounts
	 * 
	 * @param accountFile
	 * @throws FileNotFoundException
	 */
	private void loadFile(File accountFile) throws FileNotFoundException {
		try (Scanner scanner = new Scanner(accountFile)) {
			// Read first line that must be header
			String firstline = scanner.nextLine();
			List<String> headers = null;
			if (firstline != null && firstline.startsWith(CSV_COMMENT_PREFIX)) {
				headers = Arrays.asList(firstline.replace(CSV_COMMENT_PREFIX, "").split(CSV_SPLITTER));
				if (!headers.containsAll(Arrays.asList(CsvHeaderConstants.USERNAME, CsvHeaderConstants.PASSWORD, CsvHeaderConstants.EMAIL))) {
					logger.error("CSV file header is missing either username, password or email fields.");
				}
			} else {
				logger.error("CSV file is missing header line.");
				return;
			}

			// Read all other lines
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				AccountData newAccount = buildAccountDataFromCsv(line, headers);
				this.add(newAccount);
			}
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
