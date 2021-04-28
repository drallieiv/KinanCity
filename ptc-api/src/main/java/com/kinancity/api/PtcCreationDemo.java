package com.kinancity.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.cookies.SaveAllCookieJar;
import com.kinancity.api.errors.FatalException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.fatal.AccountDuplicateException;
import com.kinancity.api.errors.fatal.EmailDuplicateOrBlockedException;
import com.kinancity.api.errors.tech.AccountRateLimitExceededException;
import com.kinancity.api.errors.tech.CaptchaSolvingException;
import com.kinancity.api.model.AccountData;

import okhttp3.OkHttpClient;

/**
 * API demo
 * 
 * @author drallieiv
 *
 */
public class PtcCreationDemo {

	private static Logger logger = LoggerFactory.getLogger(PtcCreationDemo.class);

	public static void main(String[] args) {

		if (args.length < 4) {
			System.out.println("Usage : add 4 arguments \n username password email 2captchaKey");
		} else {

			/**
			 * Setup
			 */

			try {
				// Create the Account
				AccountData account = new AccountData();
				account.setUsername(args[0]);
				account.setPassword(args[1]);
				account.setEmail(args[2]);

				// Setup a provider service for Captchas
				// TODO

				// Create a HTTP Client with cookies
				OkHttpClient httclient = new OkHttpClient.Builder().cookieJar(new SaveAllCookieJar()).build();

				/**
				 * Account Creation Session
				 */

				// Initialize a PTC Creation session
				PtcSession ptc = new PtcSession(httclient);

				// 1. Check password and username before we start
				if (ptc.isAccountValid(account)) {
					// 2. Start session
					String crsfToken = ptc.sendAgeCheckAndGrabCrsfToken(account);

					// 3. Captcha
					// TODO String captcha = captchaProvider.getCaptcha();
					String captcha = "TEST";

					// 4. Account Creation
					ptc.createAccount(account, crsfToken, captcha);

				} else {
					System.out.println("Username is invalid, please use another one");
				}

			} catch (FatalException e) {
				System.out.println("This account cannot be created");
				if (e instanceof AccountDuplicateException) {
					System.out.println("An account with this username already exists");
				} else if (e instanceof EmailDuplicateOrBlockedException) {
					System.out.println("An account with this email already exists or this email is blocked");
				} else {
					System.out.println("Unknown Exception :" + e.getMessage());
					e.printStackTrace();
				}

			} catch (TechnicalException e) {
				System.out.println("A technical error happened, please retry");
				if (e instanceof AccountRateLimitExceededException) {
					System.out.println("This IP has reached maximum number of creation, must change IP or wait");
				} else if (e instanceof CaptchaSolvingException) {
					System.out.println("The Captcha service failed to provide a captcha");
				} else {
					System.out.println("Other technical exception : " + e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
}
