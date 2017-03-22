package com.kinancity.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.MissingResourceException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.TwoCaptchaService;
import com.kinancity.core.errors.AccountCreationException;
import com.kinancity.core.errors.ConfigurationException;

import lombok.Data;

@Data
public class Configuration {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CONFIG_FILE = "config.properties";

	private static Configuration instance;

	private String twoCaptchaApiKey;

	private String mailHost;
	
	private int nbThreads = 5;
	
	private CaptchaProvider captchaProvider;
	
	private boolean initDone = false;
	
	// If true, everything will be mocked
	private boolean dryRun = false;

	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
			instance.load(CONFIG_FILE);
		}

		return instance;
	}

	public void init() throws ConfigurationException{
		captchaProvider = new TwoCaptchaService(twoCaptchaApiKey, dryRun);
		initDone = true;
	}
	
	/**
	 * Check if all config are OK
	 * 
	 * @return
	 */
	public boolean checkConfiguration() {
		if(!initDone){
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

		return true;
	}

	/**
	 * Load config from prop file
	 * 
	 * @param configFile
	 */
	private void load(String configFile) {
		try {
			Properties prop = new Properties();
			InputStream in = getClass().getResourceAsStream("/" + CONFIG_FILE);
			if (in == null) {
				logger.warn("Skipping loading configuration file, you may copy config.example.properties as config.properties and edit your configuration");
				return;
			}
			prop.load(in);
			in.close();

			// Load Config
			this.setTwoCaptchaApiKey(prop.getProperty("twoCaptcha.key"));
			this.setMailHost(prop.getProperty("email.host"));

		} catch (IOException e) {
			logger.error("failed loading config.properties");
		}

	}
}
