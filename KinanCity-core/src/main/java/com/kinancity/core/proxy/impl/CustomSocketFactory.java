package com.kinancity.core.proxy.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

import sockslib.client.SocksProxy;
import sockslib.client.SocksSocket;

public class CustomSocketFactory extends SocketFactory {

	private SocksProxy proxy;

	public CustomSocketFactory(SocksProxy proxy) {
		this.proxy = proxy;
	}

	public Socket createSocket() {
		try {
			return new SocksSocket(proxy);
		} catch (IOException e) {
			return null;
		}
	}

	public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
		return createSocket();
	}

	public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
		return createSocket();
	}

	public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException {
		return createSocket();
	}

	public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
		return createSocket();
	}

}
