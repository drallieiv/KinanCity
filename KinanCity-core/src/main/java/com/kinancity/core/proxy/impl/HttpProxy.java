package com.kinancity.core.proxy.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

import org.apache.commons.lang.StringUtils;

import com.kinancity.core.proxy.HttpProxyProvider;
import com.kinancity.core.proxy.ProxyBasicAuthenticator;

import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

/**
 * Using HTTP proxy
 * 
 * @author drallieiv
 */
public class HttpProxy implements HttpProxyProvider {

	// DNS or IP address
	private String host;

	// Port
	private int port;

	// Proxy auth Login
	private String login;

	// Proxy auth password
	private String pass;

	@Override
	public OkHttpClient getClient() {
		Builder clientBuilder = new OkHttpClient.Builder();

		// HTTP Proxy
		clientBuilder.proxy(new Proxy(Type.HTTP, new InetSocketAddress(host, port)));

		// Authentication
		if (StringUtils.isNotEmpty(login)) {
			clientBuilder.proxyAuthenticator(new ProxyBasicAuthenticator(login, pass));
		}

		return clientBuilder.build();
	}
	
	public String toString(){
		return String.format(host+":"+port);
	}

}
