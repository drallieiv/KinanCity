package com.kinancity.api;

import java.io.BufferedWriter;
import java.io.IOException;
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

import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.errors.FatalException;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.api.errors.fatal.AccountDuplicateException;
import com.kinancity.api.errors.fatal.EmailDuplicateOrBlockedException;
import com.kinancity.api.errors.tech.AccountRateLimitExceededException;
import com.kinancity.api.errors.tech.HttpConnectionException;
import com.kinancity.api.errors.tech.IpSoftBanException;
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
	private final String PTC_USENAME_EXPREG = "^[a-zA-Z0-9]{6,16}$";

	private OkHttpClient client;

	@Setter
	private boolean dryRun;

	// Should the age form be really sent ?
	@Setter
	private boolean sendAgeCheck = false;

	// Dump Result
	public static final int NEVER = 0;
	public static final int ON_FAILURE = 1;
	public static final int ALWAYS = 2;
	@Setter
	private int dumpResult = 0;

	@Getter
	private CreationOptions options;

	/**
	 * Start a new PTC Session. Client must support cookies.
	 * 
	 * @param client
	 */
	public PtcSession(OkHttpClient client) {
		this.client = client;
		this.options = new CreationOptions();
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

		if (!Pattern.matches(PTC_USENAME_EXPREG, username)) {
			logger.error("Invalid username '{}', The username may contains illegal values and must be between 6 and 16 characters long", username);
			return false;
		}

		// All passed and went OK
		return true;

	}

	/**
	 * Check if the account is valid
	 * 
	 * @param account
	 * @return if the account is valid or not
	 * @throws TechnicalException
	 *             if the validation could not be done entirely
	 */
	public boolean isAccountValidWebservice(AccountData account) throws TechnicalException {

		if (dryRun) {
			logger.info("Dry-Run : Check if username and password are okay");
			return true;
		}

		String username = account.getUsername();

		// Check username validity with API
		Request request = buildUsernameCheckApiRequest(username);

		// Send a request to the Nintendo REST API
		logger.debug("Execute Request [AccountValid] on proxy {}", client.proxy());
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
				// Parse the response
				Document doc = Jsoup.parse(response.body().string());
				response.body().close();
				manageError(account, response, doc, "Validation API");
				throw new TechnicalException("Validation API, unknown error");
			}

		} catch (IOException e) {
			throw new TechnicalException("Validation API did not respond");
		}
	}

	/**
	 * Simulate new account creation age check and dump CRSF token.
	 * 
	 * @param account
	 * 
	 * @return a valid CRSF token
	 * @throws TechnicalException
	 *             if the token could not be retrieived
	 */
	public String sendAgeCheckAndGrabCrsfToken(AccountData account) throws TechnicalException {

		if (dryRun) {
			logger.info("Dry-Run : Grab CRSF token from PTC web");
			return "mockCrsfToken";
		}

		// Send HTTP Request
		logger.debug("Execute Request [AgeCheck] on proxy {}", client.proxy());
		try (Response response = client.newCall(buildAgeCheckRequest()).execute()) {

			// Parse the response
			Document doc = Jsoup.parse(response.body().string());
			response.body().close();

			if (response.isSuccessful()) {
				// Look for the CRSF
				Elements tokenField = doc.select("[name=csrfmiddlewaretoken]");

				if (tokenField.isEmpty()) {
					logger.error("CSRF Token not found");

					if (dumpResult == ALWAYS) {
						Path dumpPath = dumpResult(doc, account);
						logger.error("Response dump saved in {}", dumpPath);
					}

					throw new TechnicalException("Age verification call failed");
				} else {
					String crsfToken = tokenField.get(0).val();
					if (sendAgeCheck) {
						sendAgeCheck(account, crsfToken);
					}

					return crsfToken;
				}
			}else{
				manageError(account, response, doc, "Age verification");
				throw new TechnicalException("Age verification, unknown error");
			}
		} catch (IOException e) {
			// Will happend if connection failed or timed out
			throw new HttpConnectionException("Technical error getting CSRF Token : " + e.getMessage(), e);
		}
	}

	/**
	 * Send the age check request, it will set up a dod cookie with the date of birth
	 * 
	 * NOTE: Maybe this step may skipped by manually adding the dod cookie.
	 */
	public void sendAgeCheck(AccountData account, String crsfToken) throws TechnicalException {

		if (dryRun) {
			logger.info("Dry-Run : Pass age validation");
			return;
		}

		try {
			// Create Request
			Request request = this.buildAgeCheckSubmitRequest(account, crsfToken);

			// Send Request
			logger.debug("Sending age check request");

			logger.debug("Execute Request [sendAgeCheck] on proxy {}", client.proxy());
			try (Response response = client.newCall(request).execute()) {
				// Parse Response
				if (response.isSuccessful()) {
					logger.debug("Age check done");
					return;
				}else{
					Document doc = Jsoup.parse(response.body().string());
					response.body().close();
					manageError(account, response, doc, "Age check");
				}
			}
		} catch (IOException e) {
			// Will happend if connection failed or timed out
			throw new HttpConnectionException("Age check request failed : " + e.getMessage(), e);
		}
	}

	/**
	 * Common Error management
	 * 
	 * @param account
	 * @param response
	 * @param doc
	 * @param step
	 * @throws IpSoftBanException
	 * @throws TechnicalException
	 */
	public void manageError(AccountData account, Response response, Document doc, String step) throws IpSoftBanException, TechnicalException {
		if (response.code() == 503) {
			// Check if we encountered a 503 error
			Elements title = doc.getElementsByTag("title");
			if (title != null && title.contains("403 Forbidden")) {
				throw new IpSoftBanException(step + " HTTP 503 error with 403 Forbidden message");
			} else {
				throw new IpSoftBanException(step + " HTTP 503 error may be Ip SoftBan");
			}
		} else {
			// If it is another error
			dumpResult(doc, account);
			throw new TechnicalException(step + " Request failed. HTTP " + response.code());
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
			Request request = this.buildAccountCreationRequest(account, crsfToken, captcha, options);

			// Send Request
			logger.debug("Execute Request [createAccount] on proxy {}", client.proxy());
			try (Response response = client.newCall(request).execute()) {

				// Parse the response
				Document doc = Jsoup.parse(response.body().string());
				response.body().close();

				if (dumpResult == ALWAYS) {
					dumpResult(doc, account);
				}

				if (response.isSuccessful()) {
					checkForErrors(account, doc);
				}else{
					manageError(account, response, doc, "Creation");
				}
			}

		} catch (IOException e) {
			// Will happend if connection failed or timed out
			throw new HttpConnectionException("Create account request failed : " + e.getMessage(), e);
		}
	}

	public void checkForErrors(AccountData account, Document doc) throws FatalException, AccountRateLimitExceededException, TechnicalException, AccountDuplicateException {
		// If we get an access denied, them something is wrong with the process
		// Maybe a cookie is missing or more controls have been added.
		Elements accessDenied = doc.getElementsContainingOwnText("Access Denied");
		if (!accessDenied.isEmpty()) {
			throw new FatalException("Access Denied");
		}

		// Search if the form has error notifications
		Elements errors = doc.select(".errorlist, div.error");

		boolean isUsernameUsed = false;
		boolean isQuotaExceeded = false;
		boolean isEmailError = false;

		// If we have some
		if (!errors.isEmpty()) {

			if (dumpResult == ON_FAILURE) {
				dumpResult(doc, account);
			}

			// If there is only one that says required, it's the captcha.
			if (errors.size() == 1 && errors.get(0).child(0).text().trim().equals("This field is required.")) {
				throw new TechnicalException("Invalid or missing Captcha. Captcha may have expired, try reducing captchaMaxTotalTime");
			} else {
				// List all the errors we had
				List<String> errorMessages = new ArrayList<>();
				for (int i = 0; i < errors.size(); i++) {
					Element error = errors.get(i);
					String errorTxt = error.toString().replaceAll("<[^>]*>", "").replaceAll("[\n\r]", "").trim();

					if (errorTxt.contains("username already exists")) {
						isUsernameUsed = true;
					} else if (errorTxt.contains("Account Creation Rate Limit Exceeded")) {
						isQuotaExceeded = true;
					} else if (errorTxt.contains("There is a problem with your email address")) {
						isEmailError = true;
					}

					errorMessages.add(errorTxt);
				}
				logger.warn("{} error(s) found creating account {} : {}", errors.size(), account.getUsername(), errorMessages);

				// Throw specific exception for name duplicate
				if (isUsernameUsed) {
					throw new AccountDuplicateException();
				}

				// Throw specific exception for email blocked or duplicate
				if (isEmailError) {
					throw new EmailDuplicateOrBlockedException();
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
	}

	private Path dumpResult(Document doc, AccountData account) {
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

		return dumpName.toAbsolutePath();
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
				.headers(getHeaders())
				.method("POST", body)
				.build();
		return request;
	}

	// Http Request for account creation start page with age check
	private Request buildAgeCheckRequest() {
		return new Request.Builder()
				.url(url_ptc + pathAgeCheck)
				.headers(getHeaders())
				.build();
	}

	// Http Request for age check submit
	private Request buildAgeCheckSubmitRequest(AccountData account, String csrfToken) throws UnsupportedEncodingException {
		RequestBody body = new FormBody.Builder()
				.add("dob", account.getDob())
				.add("country", account.getCountry())
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
	private Request buildAccountCreationRequest(AccountData account, String crsfToken, String captcha, CreationOptions options) throws UnsupportedEncodingException {
		FormBody.Builder builder = new FormBody.Builder()
				// Given login and password
				.add("username", account.getUsername())
				.add("email", account.getEmail())
				.add("confirm_email", account.getEmail())
				.add("password", account.getPassword())
				.add("confirm_password", account.getPassword())

				// Technical Tokens
				.add("csrfmiddlewaretoken", crsfToken)
				.add("g-recaptcha-response", captcha)

				.add("public_profile_opt_in", "False")
				.add("screen_name", "")
				.add("terms", "on");

		if(options != null){
			if(options.isEmailOptIn()) {
				builder.add("email_opt_in", "on");
			}
		}

		RequestBody body = builder.build();



		Request request = new Request.Builder()
				.url(url_ptc + pathSignup)
				.method("POST", body)
				.headers(getHeaders())
				.build();

		return request;
	}

	// Add all HTTP headers
	public static Headers getHeaders() {

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
