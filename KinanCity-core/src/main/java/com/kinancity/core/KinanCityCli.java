package com.kinancity.core;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.TwoCaptchaService;
import com.kinancity.core.creation.PtcAccountCreator;
import com.kinancity.core.creation.PtcCreationResult;
import com.kinancity.core.data.AccountData;
import com.kinancity.core.errors.AccountCreationException;

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
			options.addOption(CliOptions.SINGLE_EMAIL.asOption());
			options.addOption(CliOptions.SINGLE_USERNAME.asOption());
			options.addOption(CliOptions.SINGLE_PASSWORD.asOption());

			// Create Multiple accounts
			options.addOption(CliOptions.MULTIPLE_ACCOUNTS.asOption());

			// Captcha key given at commandLine
			options.addOption(CliOptions.CK.asOption());

			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			Configuration config = Configuration.getInstance();
			if(cmd.hasOption(CliOptions.DRY_RUN.shortName)){
				config.setDryRun(true);
			}			

			if (cmd.hasOption(CliOptions.CK.shortName)) {
				config.setTwoCaptchaApiKey(cmd.getOptionValue(CliOptions.CK.shortName));
			}

			if (config.checkConfiguration()) {

				CaptchaProvider captchaProvider = new TwoCaptchaService(config);
				PtcAccountCreator creator = new PtcAccountCreator(config, captchaProvider);

				if (cmd.hasOption(CliOptions.SINGLE_EMAIL.shortName) && cmd.hasOption(CliOptions.SINGLE_USERNAME.shortName) && cmd.hasOption(CliOptions.SINGLE_PASSWORD.shortName)) {
					LOGGER.info("Create a single account");

					AccountData account = new AccountData();
					account.setEmail(cmd.getOptionValue(CliOptions.SINGLE_EMAIL.shortName));
					account.setUsername(cmd.getOptionValue(CliOptions.SINGLE_USERNAME.shortName));
					account.setPassword(cmd.getOptionValue(CliOptions.SINGLE_PASSWORD.shortName));

					try {
						PtcCreationResult result = creator.createAccount(account);
						LOGGER.info("DONE, success : {} {}", result.isSuccess(), result.getMessage());
					} catch (AccountCreationException e) {
						LOGGER.error("\n Account Creation Error : {}",e.getMessage());
					}
				} else if (cmd.hasOption(CliOptions.MULTIPLE_ACCOUNTS.shortName)) {
					String accountFileName = cmd.getOptionValue(CliOptions.MULTIPLE_ACCOUNTS.shortName);

					creator.createAccounts(accountFileName);

				} else {

					LOGGER.error("\ninvalid arguments\n");

					HelpFormatter formatter = new HelpFormatter();
					StringWriter out = new StringWriter();
					PrintWriter writer = new PrintWriter(out);

					String cmdLineSyntax = " (-m <email>  -u <username> -p <password>) (-a <accounts.csv> ) [-ck <captchakey>]";
					formatter.printHelp(cmdLineSyntax, options);

					String usage = out.toString();
					LOGGER.error(usage);
				}

			} else {
				LOGGER.error("Account creation failed, missing configuration");
			}
		} catch (ParseException e) {
			LOGGER.error("Command line cannot be parsed");
		}

	}

}
