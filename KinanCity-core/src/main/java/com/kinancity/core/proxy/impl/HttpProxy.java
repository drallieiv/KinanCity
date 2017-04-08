package com.kinancity.core.proxy.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.cookies.SaveAllCookieJar;
import com.kinancity.core.proxy.HttpProxyProvider;
import com.kinancity.core.proxy.ProxyBasicAuthenticator;

import lombok.Getter;
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

	public static final String URI_REGEXP = "^(?:(?<protocol>[\\w\\.\\-\\+]+):\\/{2})?" +
			"(?:(?<login>[\\w\\d\\.]+):(?<pass>[\\w\\d\\.]+)@)?" +
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
		this(host, port, login, pass, Type.HTTP);
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
	public HttpProxy(String host, int port, String login, String pass, Type type) {
		this.host = host;
		this.port = port;
		this.login = login;
		this.pass = pass;
		this.type = type;
	}

	/**
	 * Create and HTTP Proxy from an URI
	 * 
	 * @param uri
	 * @return
	 */
	public static HttpProxy fromURI(String uri) {
		Matcher matcher = Pattern.compile(URI_REGEXP).matcher(uri);
		if (matcher.find()) {
			String host = matcher.group("host");

			// Type from protocol. default to HTTP.
			Type type = StringUtils.startsWith(StringUtils.lowerCase(matcher.group("protocol")), "socks") ? Type.SOCKS : Type.HTTP;

			// Port given or default from protocol.
			int port;
			if (matcher.group("port") != null) {
				port = Integer.parseInt(matcher.group("port"));
			} else {
				if (matcher.group("protocol") == null) {
					port = 8080;
				} else {
					switch (matcher.group("protocol").toLowerCase()) {
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

			String login = matcher.group("login");
			String pass = matcher.group("pass");

			return new HttpProxy(host, port, login, pass, type);
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

	public String toString() {
		return host + ":" + port;
	}

}
