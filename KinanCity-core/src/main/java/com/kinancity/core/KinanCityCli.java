package com.kinancity.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.model.AccountData;
import com.kinancity.core.generator.account.CsvReaderAccountGenerator;
import com.kinancity.core.generator.account.SequenceAccountGenerator;
import com.kinancity.core.generator.account.SingleAccountGenerator;
import com.kinancity.core.proxy.bottleneck.ProxyNoBottleneck;
import com.kinancity.core.proxy.policies.UnlimitedUsePolicy;

public class KinanCityCli {

	private static Logger LOGGER = LoggerFactory.getLogger(KinanCityCli.class);

	public static void main(String[] args) {

		Thread t = Thread.currentThread();
		t.setName("Kinan City");

		try (InputStream bannerFile = KinanCityCli.class.getClassLoader().getResourceAsStream("banner.txt")){
			
			String banner = IOUtils.toString(bannerFile, Charset.defaultCharset());
			LOGGER.info("\n{}", banner);
		} catch (IOException e) {
			LOGGER.info(" -- Start Kinan City CLI -- ");
		}
		
		LOGGER.info(" Runtime Args \n\n  {} \n ", String.join(" ", args));

		try {

			// Prepare the CLI Options
			Options options = setupCliOptions();

			// Parse the option given in CommandLine
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			Configuration config = Configuration.getInstance();
			updateConfiguration(config, options, cmd);

			if (config.checkConfiguration()) {
				LOGGER.info("Configuration is valid. Start processing.");

				PtcAccountCreator creator = new PtcAccountCreator(config);
				creator.start();

				LOGGER.info("Done. You will find the results in {}", config.getResultLogFilename());
				System.exit(0);

			} else {
				LOGGER.error("Account creation failed, missing configuration");
				System.exit(1);
			}
		} catch (ParseException e) {
			LOGGER.error("Command line cannot be parsed {}", e.getMessage());
			System.exit(1);
		}

	}

	/**
	 * Report CLI options to configuration options
	 * 
	 * @param config
	 * @param options
	 * @param cmd
	 * @return
	 */
	private static Configuration updateConfiguration(Configuration config, Options options, CommandLine cmd) {

		// -dryrun : do everything but without real actions (captcha/creation)
		if (cmd.hasOption(CliOptions.DRY_RUN.shortName)) {
			config.setDryRun(true);
		}

		// -ck/-captchaKey : 2captcha api key
		if (cmd.hasOption(CliOptions.CK.shortName)) {
			config.setTwoCaptchaApiKey(cmd.getOptionValue(CliOptions.CK.shortName));
		}

		// -nl/-noLimit : Use Unlimited Policy
		if (cmd.hasOption(CliOptions.NO_LIMIT.shortName)) {
			config.setProxyPolicy(new UnlimitedUsePolicy());
			config.setBottleneck(new ProxyNoBottleneck());
		}

		// -px/-proxies : list of proxy to use
		if (cmd.hasOption(CliOptions.PROXIES.shortName)) {
			config.loadProxies(cmd.getOptionValue(CliOptions.PROXIES.shortName));
		}

		// -o/-output : use specific output file for results
		if (cmd.hasOption(CliOptions.OUTPUT.shortName)) {
			config.setResultLogFilename(cmd.getOptionValue(CliOptions.OUTPUT.shortName));
		}

		// -npc/-noProxyCheck : Skip proxy check at startup
		if (cmd.hasOption(CliOptions.NO_PROXY_CHECK.shortName)) {
			config.setSkipProxyTest(true);
		}

		// -t/-thread : Customize number of thread for parallel processing
		if (cmd.hasOption(CliOptions.NB_THREADS.shortName)) {
			config.setNbThreads(Integer.valueOf(cmd.getOptionValue(CliOptions.NB_THREADS.shortName)));
		}

		// 3 types of generation : unique, csv and sequence

		if (cmd.hasOption(CliOptions.SEQ_ACCOUNTS_FORMAT.shortName) && cmd.hasOption(CliOptions.SEQ_ACCOUNTS_COUNT.shortName) && cmd.hasOption(CliOptions.EMAIL.shortName) && cmd.hasOption(CliOptions.PASSWORD.shortName)) {
			LOGGER.info("Use a Sequence Account Generator");
			SequenceAccountGenerator sequenceGenerator = new SequenceAccountGenerator();
			sequenceGenerator.setBaseEmail(cmd.getOptionValue(CliOptions.EMAIL.shortName));
			sequenceGenerator.setStaticPassword(cmd.getOptionValue(CliOptions.PASSWORD.shortName));
			sequenceGenerator.setUsernamePattern(cmd.getOptionValue(CliOptions.SEQ_ACCOUNTS_FORMAT.shortName));
			sequenceGenerator.setNbAccounts(Integer.parseInt(cmd.getOptionValue(CliOptions.SEQ_ACCOUNTS_COUNT.shortName)));
			sequenceGenerator.setStartFrom(Integer.parseInt(cmd.getOptionValue(CliOptions.SEQ_ACCOUNTS_START.shortName, "0")));
			config.setAccountGenerator(sequenceGenerator);

		} else if (cmd.hasOption(CliOptions.EMAIL.shortName) && cmd.hasOption(CliOptions.SINGLE_USERNAME.shortName) && cmd.hasOption(CliOptions.PASSWORD.shortName)) {
			LOGGER.info("Create a single account");
			AccountData account = new AccountData();
			account.setEmail(cmd.getOptionValue(CliOptions.EMAIL.shortName));
			account.setUsername(cmd.getOptionValue(CliOptions.SINGLE_USERNAME.shortName));
			account.setPassword(cmd.getOptionValue(CliOptions.PASSWORD.shortName));
			config.setAccountGenerator(new SingleAccountGenerator(account));

			// No need to use multiple thread for 1 account only
			config.setNbThreads(1);
		} else if (cmd.hasOption(CliOptions.CSV_ACCOUNTS.shortName)) {
			String accountFileName = cmd.getOptionValue(CliOptions.CSV_ACCOUNTS.shortName);
			config.setAccountGenerator(new CsvReaderAccountGenerator(accountFileName));
		} else {
			LOGGER.error("invalid arguments\n");

			HelpFormatter formatter = new HelpFormatter();
			String cmdLineSyntax = " one of \n"
					+ "  -m <email>  -u <username> -p <password> \n"
					+ "  -a <accounts.csv> \n"
					+ "  -m <email> -c <#ofAccounts> -f <format**> -p <password> (-s <first#>)\n"
					+ " and optional : -ck <captchakey> \n\n";
			formatter.setWidth(180);
			formatter.printHelp(cmdLineSyntax, options);

			System.exit(1);
		}
		return config;
	}

	private static Options setupCliOptions() {
		// CLI Options
		Options options = new Options();

		// Dry Run Mode
		options.addOption(CliOptions.DRY_RUN.asOption());

		// Creates only 1 account
		options.addOption(CliOptions.EMAIL.asOption());
		options.addOption(CliOptions.SINGLE_USERNAME.asOption());
		options.addOption(CliOptions.PASSWORD.asOption());

		// Create Multiple accounts
		options.addOption(CliOptions.CSV_ACCOUNTS.asOption());

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
		options.addOption(CliOptions.NO_LIMIT.asOption());
		return options;
	}

}
