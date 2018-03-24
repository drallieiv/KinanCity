package com.kinancity.mail.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class HttpProxy {

	private static final int DEFAULT_PORT = 8080;
	private static final String DEFAULT_PROTOCOL = "http";

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
	
	// Backup list
	private List<HttpProxy> otherProxies = new ArrayList<>();

	public static final String URI_REGEXP = "^(?:(?<protocol>[\\w\\.\\-\\+]+):\\/{2})?" +
			"(?:(?<login>[\\w\\d\\.\\-%]+):(?<pass>[\\w\\d\\.\\-%]+)@)?" +
			"(?:(?<host>[a-zA-Z0-9\\.\\-_]+)" +
			"(?::(?<port>\\d{1,5}))?)$";

	static final Map<String, Integer> DEFAULT_MAPPING = Collections.unmodifiableMap(new HashMap<String, Integer>() {
		private static final long serialVersionUID = 1L;
		{
			put("http", 8080);
			put("https", 3128);
			put("socks", 1080);
			put("socks4", 1080);
			put("socks5", 1080);
		}
	});

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

			String protocol = StringUtils.lowerCase(matcher.group("protocol"));

			String login = matcher.group("login");
			String pass = matcher.group("pass");

			int port = -1;
			if (matcher.group("port") != null) {
				port = Integer.parseInt(matcher.group("port"));
			}

			// Full Default is HTTP port 8080
			if (port < 0 && protocol == null) {
				protocol = "http";
				port = DEFAULT_PORT;
			} else {
				if (protocol == null) {
					// Guess port from Protocol
					for (Entry<String, Integer> entry : DEFAULT_MAPPING.entrySet()) {
						if (entry.getValue() == port) {
							protocol = entry.getKey();
							break;
						}
					}
					if (protocol == null) {
						protocol = DEFAULT_PROTOCOL;
					}
				} else if (port < 0) {
					// Guess protocol from port
					if (DEFAULT_MAPPING.get(protocol) != null) {
						port = DEFAULT_MAPPING.get(protocol);
					} else {
						port = DEFAULT_PORT;
					}
				}
			}

			// Type from protocol. default to HTTP.
			Type type = StringUtils.startsWith(protocol, "socks") ? Type.SOCKS : Type.HTTP;

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

	public OkHttpClient getClient() {
		Builder clientBuilder = new OkHttpClient.Builder();

		// TimeOuts
		clientBuilder.connectTimeout(connectionTimeout, TimeUnit.SECONDS);
		clientBuilder.readTimeout(2 * connectionTimeout, TimeUnit.SECONDS);

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

	public String getHost() {
		return host;
	}
	
	/**
	 * Switch to another proxy
	 * @return new proxy to use or same one if no others
	 */
	public HttpProxy switchProxies(){	
		List<HttpProxy> others = this.getOtherProxies();
		if(otherProxies.isEmpty()){
			return this;
		}
		
		HttpProxy next = otherProxies.get(0);
		others.remove(next);
		others.add(this);
		this.otherProxies.clear();
		
		next.getOtherProxies().addAll(others);
		
		return next;
	}

}
