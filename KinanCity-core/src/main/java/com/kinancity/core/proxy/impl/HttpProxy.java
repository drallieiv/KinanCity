package com.kinancity.core.proxy.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.cookies.SaveAllCookieJar;
import com.kinancity.core.proxy.HttpProxyProvider;
import com.kinancity.core.proxy.ProxyBasicAuthenticator;

import lombok.Getter;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;

/**
 * Using HTTP proxy
 * 
 * @author drallieiv
 */
@Getter
public class HttpProxy implements HttpProxyProvider {

	private static Logger logger = LoggerFactory.getLogger(HttpProxy.class);

	// Protocol
	private String protocol;

	// DNS or IP address
	private String host;

	// Port
	private int port;

	// Proxy auth Login
	private String login;

	// Proxy auth password
	private String pass;

	// Type HTTP or SOCKS
	private Type type;
	
	// Connction timeout, default is 60s
	@Setter
	private int connectionTimeout = 60;

	public static final String URI_REGEXP = "^(?:(?<protocol>[\\w\\.\\-\\+]+):\\/{2})?" +
			"(?:(?<login>[\\w\\d\\.\\-]+):(?<pass>[\\w\\d\\.]+)@)?" +
			"(?:(?<host>[a-zA-Z0-9\\.\\-_]+)" +
			"(?::(?<port>\\d{1,5}))?)$";

	/**
	 * Constructor for a Http Proxy with auth
	 * 
	 * @param host
	 *            ip address or dns
	 * @param port
	 *            connection port
	 * @param login
	 *            login
	 * @param pass
	 *            password
	 */
	public HttpProxy(String host, int port, String login, String pass) {
		this(host, port, login, pass, Type.HTTP, "http");
	}

	/**
	 * Constructor for a Http Proxy with auth
	 * 
	 * @param host
	 *            ip address or dns
	 * @param port
	 *            connection port
	 * @param login
	 *            login
	 * @param pass
	 *            password
	 */
	public HttpProxy(String host, int port, String login, String pass, Type type, String protocol) {
		this.host = host;
		this.port = port;
		this.login = login;
		this.pass = pass;
		this.type = type;
		this.protocol = protocol;
	}

	/**
	 * Create and HTTP Proxy from an URI
	 * 
	 * @param uri
	 * @return
	 */
	public static HttpProxy fromURI(String uri) {
		if (uri == null) {
			return null;
		}
		Matcher matcher = Pattern.compile(URI_REGEXP).matcher(uri);
		if (matcher.find()) {
			String host = matcher.group("host");

			// Type from protocol. default to HTTP.
			Type type = StringUtils.startsWith(StringUtils.lowerCase(matcher.group("protocol")), "socks") ? Type.SOCKS : Type.HTTP;

			String login = matcher.group("login");
			String pass = matcher.group("pass");
			String protocol = matcher.group("protocol");

			// Port given or default from protocol.
			int port;
			if (matcher.group("port") != null) {
				port = Integer.parseInt(matcher.group("port"));
			} else {
				if (protocol == null) {
					port = 8080;
				} else {
					switch (protocol.toLowerCase()) {
					case "http":
						port = 8080;
						break;
					case "https":
						port = 443;
						break;
					case "socks":
					case "socks4":
					case "socks5":
						port = 1080;
						break;
					default:
						port = 8080;
						break;
					}
				}
			}

			return new HttpProxy(host, port, login, pass, type, protocol);
		} else {
			logger.warn("Cannot load URI [{}] as a HTTP Proxy", uri);
			return null;
		}
	}

	/**
	 * Constructor for a Http Proxy without auth
	 * 
	 * @param host
	 *            ip address or dns
	 * @param port
	 *            connection port
	 */
	public HttpProxy(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public OkHttpClient getClient() {
		Builder clientBuilder = new OkHttpClient.Builder();
		
		// TimeOuts
		clientBuilder.connectTimeout(connectionTimeout, TimeUnit.SECONDS);
		clientBuilder.readTimeout(2 * connectionTimeout, TimeUnit.SECONDS);

		// Own Cookie Jar
		clientBuilder.cookieJar(new SaveAllCookieJar());

		// HTTP Proxy
		clientBuilder.proxy(new Proxy(type, new InetSocketAddress(host, port)));

		// Authentication
		if (StringUtils.isNotEmpty(login)) {
			clientBuilder.proxyAuthenticator(new ProxyBasicAuthenticator(login, pass));
		}

		return clientBuilder.build();
	}

	public String toURI() {
		StringBuilder sb = new StringBuilder();
		if (protocol != null) {
			sb.append(protocol).append("://");
		}
		if (login != null) {
			sb.append(login).append(":").append(pass).append("@");
		}
		sb.append(host);
		if (port > 0) {
			sb.append(":").append(port);
		}
		return sb.toString();
	}

	public String toString() {
		return toURI();
	}

}