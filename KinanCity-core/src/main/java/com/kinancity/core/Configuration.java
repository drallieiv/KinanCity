package com.kinancity.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.MockCaptchaService;
import com.kinancity.core.captcha.TwoCaptchaService;
import com.kinancity.core.errors.ConfigurationException;
import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.proxy.ProxyManager;
import com.kinancity.core.proxy.ProxyTester;
import com.kinancity.core.proxy.impl.HttpProxy;
import com.kinancity.core.proxy.impl.NoProxy;
import com.kinancity.core.proxy.policies.NintendoTimeLimitPolicy;
import com.kinancity.core.proxy.policies.ProxyPolicy;

import lombok.Data;

@Data
public class Configuration {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CONFIG_FILE = "config.properties";

	private static Configuration instance;

	private String twoCaptchaApiKey;

	private int nbThreads = 5;

	private CaptchaProvider captchaProvider;

	private ProxyManager proxyManager;

	private ProxyPolicy proxyPolicy;

	private String resultLogFilename = "result.csv";

	private PrintWriter resultLogWriter;

	private boolean initDone = false;
	
	private boolean skipProxyTest = false;
	
	private int maxRetry = 3;
	

	// If true, everything will be mocked
	private boolean dryRun = false;

	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
			instance.load(CONFIG_FILE);
		}

		return instance;
	}

	public void init() throws ConfigurationException {

		try {
			
			if (captchaProvider == null) {
				captchaProvider = new TwoCaptchaService(twoCaptchaApiKey, dryRun);
			}

			if (proxyManager == null) {
				logger.info("ProxyManager using direction connection with Nintendo policy");
				proxyManager = new ProxyManager();
				// Add Direct connection
				proxyManager.addProxy(new ProxyInfo(new NintendoTimeLimitPolicy(), new NoProxy()));
			}

			resultLogWriter = new PrintWriter(new FileWriter(resultLogFilename, true));
		} catch (IOException e) {
			throw new ConfigurationException(e);
		}

		initDone = true;
	}

	/**
	 * Check if all config are OK
	 * 
	 * @return
	 */
	public boolean checkConfiguration() {

		if (!initDone) {
			try {
				init();
			} catch (ConfigurationException e) {
				logger.error("Configuration Init Failed", e);
				return false;
			}
		}

		if (captchaProvider == null) {
			return false;
		}

		if(!skipProxyTest){
			logger.info("Validating given proxies");
	
			ProxyTester proxyTester = new ProxyTester();
			List<ProxyInfo> invalidProxies = new ArrayList<>();
			for (ProxyInfo proxy : proxyManager.getProxies()) {
				if (!proxyTester.testProxy(proxy.getProvider())) {
					logger.warn("Proxy test for {} failed, remove proxy", proxy.getProvider());
					invalidProxies.add(proxy);
				}
			}
			if(invalidProxies.isEmpty()){
				logger.info("All proxies are valid");
			}else{
				proxyManager.getProxies().removeAll(invalidProxies);
			}
			if (proxyManager.getProxies().isEmpty()) {
				logger.error("No valid proxy given");
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Load config from prop file
	 * 
	 * @param configFilePath
	 */
	private void load(String configFilePath) {
		try {
			Properties prop = new Properties();
			File configFile = new File(CONFIG_FILE);

			if (!configFile.exists()) {
				logger.warn("Skipping loading configuration file, you may copy config.example.properties as config.properties and edit your configuration");
				return;
			}

			InputStream in = new FileInputStream(configFile);
			prop.load(in);
			in.close();

			// Load Config
			this.setTwoCaptchaApiKey(prop.getProperty("twoCaptcha.key"));
			this.loadProxies(prop.getProperty("proxies"));

		} catch (IOException e) {
			logger.error("failed loading config.properties");
		}

	}

	public void loadProxies(String proxiesConfig) {

		if (proxiesConfig != null) {
			
			proxiesConfig = proxiesConfig.replaceAll("[\\[\\]]", "");
			
			proxyManager = new ProxyManager();

			String[] proxyDefs = proxiesConfig.split("[,;]");
			for (String proxyDef : proxyDefs) {
				String[] parts = proxyDef.split("[:@]");
				if (parts.length == 4) {
					try {
						// Auth Proxy
						String host = parts[2];
						int port = Integer.parseInt(parts[3]);
						String login = parts[0];
						String pass = parts[1];
						proxyManager.addProxy(new ProxyInfo(getProxyPolicyInstance(), new HttpProxy(host, port, login, pass)));
					} catch (NumberFormatException e) {
						logger.error("Invalid proxy {}", proxyDef);
					}
				} else if (parts.length == 2) {
					try {
						// Standard HTTP Proxy
						String host = parts[0];
						int port = Integer.parseInt(parts[1]);
						proxyManager.addProxy(new ProxyInfo(getProxyPolicyInstance(), new HttpProxy(host, port)));
					} catch (NumberFormatException e) {
						logger.error("Invalid proxy {}", proxyDef);
					}
				}
			}

			logger.info("ProxyManager setup with {} proxies", proxyManager.getProxies().size());
		}

	}

	private ProxyPolicy getProxyPolicyInstance() {
		if (proxyPolicy == null) {
			proxyPolicy = new NintendoTimeLimitPolicy();
			// proxyPolicy = new TimeLimitPolicy(3, 20);
		}
		return proxyPolicy.clone();
	}

}
