package com.kinancity.core;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.creation.PtcAccountCreator;
import com.kinancity.core.creation.PtcCreationResult;
import com.kinancity.core.creation.PtcCreationSummary;
import com.kinancity.core.data.AccountData;
import com.kinancity.core.errors.AccountCreationException;
import com.kinancity.core.generator.impl.SequenceAccountGenerator;

public class KinanCityCli {

	private static Logger LOGGER = LoggerFactory.getLogger(KinanCityCli.class);

	public static void main(String[] args) {

		Thread t = Thread.currentThread();
		t.setName("Kinan City");

		try {
			LOGGER.info(" -- Start Kinan City CLI -- ");

			// CLI Options
			Options options = new Options();

			// Dry Run Mode
			options.addOption(CliOptions.DRY_RUN.asOption());

			// Creates only 1 account
			options.addOption(CliOptions.EMAIL.asOption());
			options.addOption(CliOptions.SINGLE_USERNAME.asOption());
			options.addOption(CliOptions.PASSWORD.asOption());

			// Create Multiple accounts
			options.addOption(CliOptions.MULTIPLE_ACCOUNTS.asOption());

			// Create a Sequence of accounts
			options.addOption(CliOptions.SEQ_ACCOUNTS_COUNT.asOption());
			options.addOption(CliOptions.SEQ_ACCOUNTS_FORMAT.asOption());
			options.addOption(CliOptions.SEQ_ACCOUNTS_START.asOption());

			// Captcha key given at commandLine
			options.addOption(CliOptions.CK.asOption());

			// Number of Threads
			options.addOption(CliOptions.NB_THREADS.asOption());

			// Proxies
			options.addOption(CliOptions.PROXIES.asOption());
			options.addOption(CliOptions.NO_PROXY_CHECK.asOption());

			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			// Load Config Options

			Configuration config = Configuration.getInstance();

			if (cmd.hasOption(CliOptions.DRY_RUN.shortName)) {
				config.setDryRun(true);
			}

			if (cmd.hasOption(CliOptions.CK.shortName)) {
				config.setTwoCaptchaApiKey(cmd.getOptionValue(CliOptions.CK.shortName));
			}

			if (cmd.hasOption(CliOptions.PROXIES.shortName)) {
				config.loadProxies(cmd.getOptionValue(CliOptions.PROXIES.shortName));
			}

			if (cmd.hasOption(CliOptions.OUTPUT.shortName)) {
				config.setResultLogFilename(cmd.getOptionValue(CliOptions.OUTPUT.shortName));
			}

			if (cmd.hasOption(CliOptions.NO_PROXY_CHECK.shortName)) {
				config.setSkipProxyTest(true);
			}

			if (cmd.hasOption(CliOptions.NB_THREADS.shortName)) {
				config.setNbThreads(Integer.valueOf(cmd.getOptionValue(CliOptions.NB_THREADS.shortName)));
			}

			if (config.checkConfiguration()) {

				PtcAccountCreator creator = new PtcAccountCreator(config);

				if (cmd.hasOption(CliOptions.SEQ_ACCOUNTS_FORMAT.shortName) && cmd.hasOption(CliOptions.SEQ_ACCOUNTS_COUNT.shortName) && cmd.hasOption(CliOptions.EMAIL.shortName) && cmd.hasOption(CliOptions.PASSWORD.shortName)) {

					SequenceAccountGenerator generator = new SequenceAccountGenerator();
					generator.setBaseEmail(cmd.getOptionValue(CliOptions.EMAIL.shortName));
					generator.setStaticPassword(cmd.getOptionValue(CliOptions.PASSWORD.shortName));

					generator.setUsernamePattern(cmd.getOptionValue(CliOptions.SEQ_ACCOUNTS_FORMAT.shortName));
					generator.setNbAccounts(Integer.parseInt(cmd.getOptionValue(CliOptions.SEQ_ACCOUNTS_COUNT.shortName)));
					generator.setStartFrom(Integer.parseInt(cmd.getOptionValue(CliOptions.SEQ_ACCOUNTS_START.shortName, "0")));

					List<AccountData> accountsToCreate = new ArrayList<>();
					AccountData account;
					while ((account = generator.nextAccountData()) != null) {
						accountsToCreate.add(account);
					}

					try {
						PtcCreationSummary summary = creator.createAccounts(accountsToCreate);

						LOGGER.info(" All creations DONE : {}", summary);
						System.exit(0);
					} catch (AccountCreationException e) {
						LOGGER.error("\n Account Creation Error : {}", e.getMessage());
						System.exit(1);
					}

				} else if (cmd.hasOption(CliOptions.EMAIL.shortName) && cmd.hasOption(CliOptions.SINGLE_USERNAME.shortName) && cmd.hasOption(CliOptions.PASSWORD.shortName)) {
					LOGGER.info("Create a single account");

					AccountData account = new AccountData();
					account.setEmail(cmd.getOptionValue(CliOptions.EMAIL.shortName));
					account.setUsername(cmd.getOptionValue(CliOptions.SINGLE_USERNAME.shortName));
					account.setPassword(cmd.getOptionValue(CliOptions.PASSWORD.shortName));

					try {
						PtcCreationResult result = creator.createAccount(account);
						LOGGER.info("DONE : {}", result.getMessage());
						System.exit(0);
					} catch (AccountCreationException e) {
						LOGGER.error("Account Creation Error : {}", e.getMessage());
						System.exit(1);
					}
				} else if (cmd.hasOption(CliOptions.MULTIPLE_ACCOUNTS.shortName)) {
					String accountFileName = cmd.getOptionValue(CliOptions.MULTIPLE_ACCOUNTS.shortName);

					PtcCreationSummary summary = creator.createAccounts(accountFileName);

					LOGGER.info(" All creations DONE : {}", summary);
					System.exit(0);

				} else {

					LOGGER.error("invalid arguments\n");

					HelpFormatter formatter = new HelpFormatter();
					StringWriter out = new StringWriter();
					PrintWriter writer = new PrintWriter(out);

					String cmdLineSyntax = " one of \n"
							+ "  -m <email>  -u <username> -p <password> \n"
							+ "  -a <accounts.csv> \n"
							+ "  -m <email> -c <#ofAccounts> -f <format**> -p <password> (-s <first#>)\n"
							+ " and optional : -ck <captchakey> \n\n";
					formatter.setWidth(180);
					formatter.printHelp(cmdLineSyntax, options);

					System.exit(0);
				}

			} else {
				LOGGER.error("Account creation failed, missing configuration");
				System.exit(1);
			}
		} catch (ParseException e) {
			LOGGER.error("Command line cannot be parsed {}", e.getMessage());
			System.exit(1);
		}

	}

}
