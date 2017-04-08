package com.kinancity.core.proxy.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Proxy.Type;

import org.junit.Test;

public class HttpProxyTest {

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
