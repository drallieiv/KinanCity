package com.pallettown.core.captcha;

import java.io.IOException;
import java.net.CookieManager;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pallettown.core.errors.CaptchaSolvingException;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class TwoCaptchaService implements CaptchaProvider {

	private static final String NOT_READY = "NOT_READY";

	private static final String OK_RESPONSE_PREFIX = "OK|";

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * The URL where the recaptcha is placed. For example:
	 * https://www.google.com/recaptcha/api2/demo
	 */
	private String pageUrl;

	/**
	 * Captcha API key
	 */
	private String apiKey;
	
	/**
	 * How many time should we wait in seconds
	 */
	private int maxTotalTime = 60;
	
	/**
	 * Delay between requests in ms
	 */
	private int spleepTime = 1000;
	
	/**
	 * Google Site key
	 */
	private String googleSiteKey = "6LdpuiYTAAAAAL6y9JNUZzJ7cF3F8MQGGKko1bCy";

	// URLs
	private static final String PAGE_URL = "https://club.pokemon.com/us/pokemon-trainer-club/parents/sign-up";
	private static final String CAPTCHA_IN = "http://2captcha.com/in.php";
	private static final String CAPTCHA_OUT = "http://2captcha.com/res.php";

	private OkHttpClient captchaClient;

	public TwoCaptchaService(String apiKey) {
		this.apiKey = apiKey;
		this.captchaClient = new OkHttpClient();
	}

	@Override
	public String getCaptcha() throws CaptchaSolvingException {	
		
		if(this.apiKey.equals("mock")){
			return "mockCaptcha";
		}
		
		Request sendRequest = buildSendCaptchaRequest();

		try {
			Response sendResponse = captchaClient.newCall(sendRequest).execute();
			String body = sendResponse.body().string();
			sendResponse.body().close();
			if(body != null && body.startsWith(OK_RESPONSE_PREFIX)){
				
				String captchaId = body.substring(OK_RESPONSE_PREFIX.length());
								
				Request resolveRequest = buildReceiveCaptchaRequest(captchaId);
				
				logger.info("Captcha sent to 2captcha, id: {}. Waiting for a response", captchaId);
				StopWatch time = new StopWatch();
				time.start();
				
				int spinnerCount = 0;
				
				while(time != null && time.getTime() < maxTotalTime * 1000){
					Response solveResponse = captchaClient.newCall(resolveRequest).execute();
					String solution = solveResponse.body().string();
					solveResponse.body().close();
										
					if(solution.contains(NOT_READY)){
						// Keep Waiting
						spinnerCount++;
						if(spinnerCount % 10 == 9){
							logger.info("...");
						}						
						Thread.sleep(spleepTime);
					}else{
						// Stop loop
						time.stop();
						logger.debug("Response received from 2captcha in {}s", time.getTime()/1000);
						
						if(solution.startsWith(OK_RESPONSE_PREFIX)){
							return solution.substring(OK_RESPONSE_PREFIX.length());
						}
						
						throw new CaptchaSolvingException("2 Captcha Error, solution not OK : " + solution);
					}
				}
				
				
			}else {
				throw new CaptchaSolvingException("2 Captcha Error : " + body);
			}
		} catch (IOException e) {
			throw new CaptchaSolvingException(e);
		} catch (InterruptedException e) {
			throw new CaptchaSolvingException(e);
		}

		return null;
	}

	/**
	 * Send Captcha Request
	 * @return
	 */
	private Request buildSendCaptchaRequest() {
		HttpUrl url = HttpUrl.parse(CAPTCHA_IN).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("method", "userrecaptcha")
				.addQueryParameter("googlekey", googleSiteKey)
				.addQueryParameter("pageurl", PAGE_URL)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.build();
		return request;
	}
	

	/**
	 * Receive Captcha Request
	 * @param catpchaId 
	 * @return
	 */
	private Request buildReceiveCaptchaRequest(String catpchaId) {
		HttpUrl url = HttpUrl.parse(CAPTCHA_OUT).newBuilder()
				.addQueryParameter("key", apiKey)
				.addQueryParameter("action", "get")
				.addQueryParameter("id", catpchaId)
				.build();

		Request request = new Request.Builder()
				.url(url)
				.build();
		return request;
	}

}
