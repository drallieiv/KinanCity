package com.kinancity.core.proxy;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProxyTester {

	private static final int CONNECT_TIMEOUT = 45;

	private Logger logger = LoggerFactory.getLogger(getClass());

	private String url_ptc = "https://club.pokemon.com/us/pokemon-trainer-club";

	public boolean testProxy(HttpProxyProvider provider) {

		try {
			Request testrequest = new Request.Builder().url(url_ptc).get().build();

			OkHttpClient client = provider.getClient().newBuilder().connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS).build();
			try (Response response = client.newCall(testrequest).execute()) {
				if (!response.isSuccessful()) {
					logger.error("Error, received HTTP {}", response.code());
					return false;
				}
			}
			// Else the test is a success
			return true;
		} catch (SocketTimeoutException e) {
			logger.error("Proxy Timed out : {}", e.getMessage());
			return false;
		} catch (IOException e) {
			logger.debug("Proxy Test Failed : {}", e.getMessage());
			return false;
		}

	}
}
