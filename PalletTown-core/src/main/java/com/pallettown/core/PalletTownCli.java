package com.pallettown.core;

import java.io.File;
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

import com.pallettown.core.captcha.CaptchaProvider;
import com.pallettown.core.captcha.TwoCaptchaService;
import com.pallettown.core.data.AccountData;
import com.pallettown.core.errors.AccountCreationException;

public class PalletTownCli {

	private static Logger LOGGER = LoggerFactory.getLogger(PalletTownCli.class);

	public static void main(String[] args) {

		try {
			LOGGER.info(" -- Start Pallet Town CLI -- ");

			// CLI Options
			Options options = new Options();

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

			if (cmd.hasOption(CliOptions.CK.shortName)) {
				config.setTwoCaptchaApiKey(cmd.getOptionValue(CliOptions.CK.shortName));
			}

			if (config.checkConfiguration()) {

				CaptchaProvider captchaProvider = new TwoCaptchaService(config.getTwoCaptchaApiKey());
				PTCAccountCreator creator = new PTCAccountCreator(captchaProvider);

				if (cmd.hasOption(CliOptions.SINGLE_EMAIL.shortName) && cmd.hasOption(CliOptions.SINGLE_USERNAME.shortName) && cmd.hasOption(CliOptions.SINGLE_PASSWORD.shortName)) {
					LOGGER.info("Create a single account");

					AccountData account = new AccountData();
					account.setEmail(cmd.getOptionValue(CliOptions.SINGLE_EMAIL.shortName));
					account.setUsername(cmd.getOptionValue(CliOptions.SINGLE_USERNAME.shortName));
					account.setPassword(cmd.getOptionValue(CliOptions.SINGLE_PASSWORD.shortName));

					try {
						creator.createAccount(account);
						LOGGER.info("DONE");
					} catch (AccountCreationException e) {
						LOGGER.error("\n Account Creation Failed : {}",e.getMessage());
					}
				} else if (cmd.hasOption(CliOptions.MULTIPLE_ACCOUNTS.shortName)) {
					String accountFileName = cmd.getOptionValue(CliOptions.MULTIPLE_ACCOUNTS.shortName);
				
					creator.createAccounts(accountFileName);
					
				} else {

					LOGGER.error("\ninvalid arguments\n");

					HelpFormatter formatter = new HelpFormatter();
					StringWriter out = new StringWriter();
					PrintWriter writer = new PrintWriter(out);

					String cmdLineSyntax = " -m <email>  -u <username> -pdw <password> [-ck <captchakey>]";
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
