package com.kinancity.core.proxy.impl;

import com.kinancity.api.cookies.SaveAllCookieJar;
import com.kinancity.core.proxy.HttpProxyProvider;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

public class NoProxy implements HttpProxyProvider {


	@Override
	public OkHttpClient getClient() {
		Builder clientBuilder = new OkHttpClient.Builder();
		
		// Own Cookie Jar
		clientBuilder.cookieJar(new SaveAllCookieJar());
		
		return clientBuilder.build();
	}

	public String toString(){
		return "Direct";
	}
}
