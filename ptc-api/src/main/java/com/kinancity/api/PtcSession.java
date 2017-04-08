package com.kinancity.api;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.FatalException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.fatal.AccountDuplicateException;
import com.kinancity.api.errors.tech.AccountRateLimitExceededException;
import com.kinancity.api.errors.tech.HttpConnectionException;
import com.kinancity.api.model.AccountData;

import lombok.Setter;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Session for creating a PTC account.
 * 
 * @author drallieiv
 *
 */
public class PtcSession {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String url_ptc = "https://club.pokemon.com/us/pokemon-trainer-club";
	private String url_verify_api = "https://club.pokemon.com/api/signup/verify-username";

	private String pathAgeCheck = "/sign-up/";
	private String pathSignup = "/parents/sign-up";

	private final String PTC_PWD_EXPREG = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[#?!@$%^&><+`*()\\-\\]])[A-Za-z0-9#?!@$%^&><+`*()\\-\\]]{8,50}$";

	private OkHttpClient client;

	@Setter
	private boolean dryRun;

	// Dump Result
	public static final int NEVER = 0;
	public static final int ON_FAILURE = 1;
	public static final int ALWAYS = 2;
	@Setter
	private int dumpResult = 0;

	/**
	 * Start a new PTC Session. Client must support cookies.
	 * 
	 * @param client
	 */
	public PtcSession(OkHttpClient client) {
		this.client = client;
	}

	/**
	 * Check if the account is valid
	 * 
	 * @param account
	 * @return if the account is valid or not
	 * @throws TechnicalException
	 *             if the validation could not be done entirely
	 */
	public boolean isAccountValid(AccountData account) throws TechnicalException {

		if (dryRun) {
			logger.info("Dry-Run : Check if username and password are okay");
			return true;
		}

		String password = account.getPassword();
		String username = account.getUsername();

		// Check password validity
		if (!Pattern.matches(PTC_PWD_EXPREG, password)) {
			logger.error("Invalid password '{}', The password must include uppercase and lowercase letters, numbers, and symbols between 8 and 50 chars", password);
			return false;
		}

		// Check username validity with API
		Request request = buildUsernameCheckApiRequest(username);

		// Send a request to the Nintendo REST API
		try (Response response = client.newCall(request).execute()) {
			if (response.isSuccessful()) {

				// Read the response
				JsonObject jsonResponse = Json.createReader(response.body().byteStream()).readObject();
				response.body().close();

				if (!jsonResponse.getBoolean("valid")) {
					logger.error("Given username '{}' is not valid", username);
					return false;
				}

				if (jsonResponse.getBoolean("inuse")) {
					logger.error("Given username '{}' is already used, suggestions are {}", username, jsonResponse.getJsonArray("suggestions"));
					return false;
				}

				// All passed and went OK
				return true;

			} else {
				logger.error("Validation API response is an HTTP{} error", response.code());
				throw new TechnicalException("Validation API did not respond");
			}

		} catch (IOException e) {
			throw new TechnicalException("Validation API did not respond");
		}
	}

	/**
	 * Simulate new account creation age check and dump CRSF token.
	 * 
	 * @return a valid CRSF token
	 * @throws TechnicalException
	 *             if the token could not be retrieived
	 */
	public String sendAgeCheckAndGrabCrsfToken() throws TechnicalException {

		if (dryRun) {
			logger.info("Dry-Run : Grab CRSF token from PTC web");
			return "mockCrsfToken";
		}

		// Send HTTP Request
		try (Response response = client.newCall(buildAgeCheckRequest()).execute()) {

			if (response.isSuccessful()) {
				// Parse the response
				Document doc = Jsoup.parse(response.body().string());

				// Look for the CRSF
				Elements tokenField = doc.select("[name=csrfmiddlewaretoken]");

				if (tokenField.isEmpty()) {
					logger.error("CSRF Token not found");
				} else {
					String crsfToken = tokenField.get(0).val();
					sendAgeCheck(crsfToken);
					return crsfToken;
				}
			}
			throw new TechnicalException("Age verification call or CSRF extraction failed");
		} catch (IOException e) {
			// Will happend if connection failed or timed out
			throw new HttpConnectionException("Technical error getting CSRF Token", e);
		}
	}

	/**
	 * Send the age check request, it will set up a dod cookie with the date of birth
	 * 
	 * NOTE: Maybe this step may skipped by manually adding the dod cookie.
	 */
	public void sendAgeCheck(String crsfToken) throws TechnicalException {

		if (dryRun) {
			logger.info("Dry-Run : Pass age validation");
			return;
		}

		try {
			// Create Request
			Request request = this.buildAgeCheckSubmitRequest(crsfToken);

			// Send Request
			logger.debug("Sending age check request");

			try (Response response = client.newCall(request).execute()) {
				// Parse Response
				if (response.isSuccessful()) {
					logger.debug("Age check done");
					return;
				}
				throw new TechnicalException("Age check request failed");
			}
		} catch (IOException e) {
			// Will happend if connection failed or timed out
			throw new HttpConnectionException("Age check request failed", e);
		}
	}

	/**
	 * The account creation itself
	 * 
	 * @throws TechnicalException
	 * @throws FatalException
	 */
	public void createAccount(AccountData account, String crsfToken, String captcha) throws TechnicalException, FatalException {

		if (dryRun) {
			logger.info("Dry-Run : Send creation request for account [{}]", account);
			return;
		}

		try {
			// Create Request
			Request request = this.buildAccountCreationRequest(account, crsfToken, captcha);

			// Send Request
			logger.debug("Sending creation request");
			try (Response response = client.newCall(request).execute()) {

				if (response.isSuccessful()) {

					// Parse the response
					Document doc = Jsoup.parse(response.body().string());
					response.body().close();

					if (dumpResult == ALWAYS) {
						dumpResult(doc, account);
					}

					// If we get an access denied, them something is wrong with
					// the process
					// Maybe a cookie is missing or more controls have been
					// added.
					Elements accessDenied = doc.getElementsContainingOwnText("Access Denied");
					if (!accessDenied.isEmpty()) {
						throw new FatalException("Access Denied");
					}

					// Search if the form has error notifications
					Elements errors = doc.select(".errorlist");

					boolean isUsernameUsed = false;
					boolean isQuotaExceeded = false;

					// If we have some
					if (!errors.isEmpty()) {

						if (dumpResult == ON_FAILURE) {
							dumpResult(doc, account);
						}
						
						// Specific check for email error
						if(doc.getElementById("id_email").hasClass("alert-error")){
							logger.warn("Email Error, this could be IP throttle, consider as Account Rate Limited");
							throw new AccountRateLimitExceededException();
						}
						

						// If there is only one that says required, it's the captcha.
						if (errors.size() == 1 && errors.get(0).child(0).text().trim().equals("This field is required")) {
							throw new TechnicalException("Invalid or missing Captcha");
						} else {
							// List all the errors we had
							List<String> errorMessages = new ArrayList<>();
							for (int i = 0; i < errors.size(); i++) {
								Element error = errors.get(i);
								String errorTxt = error.toString().replaceAll("<[^>]*>", "").replaceAll("[\n\r]", "").trim();

								if (errorTxt.contains("username already exists")) {
									isUsernameUsed = true;
								} else if (errorTxt.contains("exceed")) {
									isQuotaExceeded = true;
								}

								errorMessages.add(errorTxt);
							}
							logger.warn("{} error(s) found creating account {} : {}", errors.size(), account.username, errorMessages);

							// Throw specific exception for name duplicate
							if (isUsernameUsed) {
								throw new AccountDuplicateException();
							}

							// Throw specific exception for quota exceeded
							if (isQuotaExceeded) {
								throw new AccountRateLimitExceededException();
							}

							// Else we have another unknown error
							throw new TechnicalException("Unknown creation error : " + errorMessages);
						}
					}

					logger.debug("SUCCESS : Account created");

				} else {
					throw new TechnicalException("PTC server bad response, HTTP " + response.code());
				}
			}

		} catch (IOException e) {
			// Will happend if connection failed or timed out
			throw new HttpConnectionException("Create account request failed", e);
		}
	}

	private void dumpResult(Document doc, AccountData account) {
		String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
		if (Files.notExists(Paths.get("dump"))) {
			try {
				Files.createDirectory(Paths.get("dump"));
			} catch (IOException e) {
				logger.warn("Cannot create dump folder");
			}
		}
		Path dumpName = Paths.get("dump/" + account.getUsername() + "_" + time + ".html");
		try (BufferedWriter writer = Files.newBufferedWriter(dumpName)) {
			String html = doc.outerHtml();
			// Cleanup Scripts 
			html = html.replaceAll("<script\\b[^<]*(?:(?!<\\/script>)<[^<]*)*<\\/script>", "");
			writer.write(html);
		} catch (IOException e) {
			logger.warn("Error dumping file {}", dumpName);
		}
	}

	/**
	 * Below are the requests used
	 */

	// Request to the REST Api for username validation
	private Request buildUsernameCheckApiRequest(String username) {
		String payload = Json.createObjectBuilder().add("name", username).build().toString();
		RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), payload);

		Request request = new Request.Builder()
				.url(url_verify_api)
				.method("POST", body)
				.build();
		return request;
	}

	// Http Request for account creation start page with age check
	private Request buildAgeCheckRequest() {
		return new Request.Builder().url(url_ptc + pathAgeCheck).headers(getHeaders()).build();
	}

	// Http Request for age check submit
	private Request buildAgeCheckSubmitRequest(String csrfToken) throws UnsupportedEncodingException {
		RequestBody body = new FormBody.Builder()
				.add("dob", "1985-01-16")
				.add("country", "US")
				.add("csrfmiddlewaretoken", csrfToken)
				.build();

		Request request = new Request.Builder()
				.url(url_ptc + pathAgeCheck)
				.method("POST", body)
				.headers(getHeaders())
				.build();

		return request;
	}

	// Http Request form the account creation itself
	private Request buildAccountCreationRequest(AccountData account, String crsfToken, String captcha) throws UnsupportedEncodingException {
		RequestBody body = new FormBody.Builder()
				// Given login and password
				.add("username", account.username)
				.add("email", account.email)
				.add("confirm_email", account.email)
				.add("password", account.password)
				.add("confirm_password", account.password)

				// Technical Tokens
				.add("csrfmiddlewaretoken", crsfToken)
				.add("g-recaptcha-response", captcha)

				.add("public_profile_opt_in", "False")
				.add("screen_name", "")
				.add("terms", "on")
				.build();

		Request request = new Request.Builder()
				.url(url_ptc + pathSignup)
				.method("POST", body)
				.headers(getHeaders())
				.build();

		return request;
	}

	// Add all HTTP headers
	private Headers getHeaders() {

		Map<String, String> headersMap = new HashMap<>();
		// Base browser User Agent
		headersMap.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");

		// CORS
		headersMap.put("Origin", "https://club.pokemon.com");
		headersMap.put("Referer", "https://club.pokemon.com/us/pokemon-trainer-club/parents/sign-up");
		headersMap.put("Upgrade-Insecure-Requests", "1");

		// Do Not Track
		headersMap.put("DNT", "1");

		// Technical exchange
		headersMap.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		headersMap.put("Accept-Language", "en-GB,en-US;q=0.8,en;q=0.6");

		return Headers.of(headersMap);
	}

}
