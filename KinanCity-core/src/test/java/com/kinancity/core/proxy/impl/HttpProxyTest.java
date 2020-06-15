package com.kinancity.core.proxy.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.Proxy.Type;

import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpProxyTest {

	@Test
	public void socksTest() throws IOException {
		String proxyUrl = "socks5://proxyfish300:bne395h1@198.41.113.2:12194";
		HttpProxy proxy = HttpProxy.fromURI(proxyUrl);
		
		OkHttpClient client = proxy.getClient();

		Request request = new Request.Builder().url("http://google.com").build();
		Response response = client.newCall(request).execute();
		assertThat(response.isSuccessful()).isTrue();
	}

	/**
	 * Build an HTTP Proxy from URI
	 */
	@Test
	public void noProtocolUriTest() {
		// Given
		String uri = "user:pass@127.0.0.1:3128";
		// When
		HttpProxy proxy = HttpProxy.fromURI(uri);
		// Then
		assertThat(proxy.getType()).isEqualTo(Type.HTTP);
		assertThat(proxy.getLogin()).isEqualTo("user");
		assertThat(proxy.getPass()).isEqualTo("pass");
		assertThat(proxy.getHost()).isEqualTo("127.0.0.1");
		assertThat(proxy.getPort()).isEqualTo(3128);
	}

	/**
	 * Encoded char in username
	 */
	@Test
	public void encodedCharUriTest() {
		// Given
		String uri = "user:p%40ss@127.0.0.1:3128";
		// When
		HttpProxy proxy = HttpProxy.fromURI(uri);
		// Then
		assertThat(proxy).isNotNull();
	}

	/**
	 * Build an HTTP Proxy from URI
	 */
	@Test
	public void noAuthNoPortUriTest() {
		// Given
		String uri = "socks5://127.0.0.1";
		// When
		HttpProxy proxy = HttpProxy.fromURI(uri);
		// Then
		assertThat(proxy.getType()).isEqualTo(Type.SOCKS);
		assertThat(proxy.getLogin()).isNull();
		assertThat(proxy.getPass()).isNull();
		assertThat(proxy.getHost()).isEqualTo("127.0.0.1");
		assertThat(proxy.getPort()).isEqualTo(1080);
	}

	/**
	 * Build an HTTP Proxy from URI
	 */
	@Test
	public void minimalUriTest() {
		// Given
		String uri = "127.0.0.1";
		// When
		HttpProxy proxy = HttpProxy.fromURI(uri);
		// Then
		assertThat(proxy.getType()).isEqualTo(Type.HTTP);
		assertThat(proxy.getLogin()).isNull();
		assertThat(proxy.getPass()).isNull();
		assertThat(proxy.getHost()).isEqualTo("127.0.0.1");
		assertThat(proxy.getPort()).isEqualTo(8080);
	}
}
