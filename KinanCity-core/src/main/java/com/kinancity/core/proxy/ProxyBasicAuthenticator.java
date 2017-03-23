package com.kinancity.core.proxy;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class ProxyBasicAuthenticator implements Authenticator {

	// HTTP Header for Proxy Auth
	private static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
	
	private String login;
	private String password;

	public ProxyBasicAuthenticator(String login, String password) {
		this.login = login;
		this.password = password;
	}

	@Override
	public Request authenticate(Route route, Response response) throws IOException {
		if (response.request().header(PROXY_AUTHORIZATION) != null) {
			return null; // Give up, we've already failed to authenticate.
		}
		String credential = Credentials.basic(login, password);
		return response.request().newBuilder().header(PROXY_AUTHORIZATION, credential).build();
	}

}
