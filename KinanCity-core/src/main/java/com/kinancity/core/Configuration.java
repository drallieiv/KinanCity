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

import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.api.PtcSession;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.captcha.client.ClientProvider;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaProvider;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.antiCaptcha.AntiCaptchaProvider;
import com.kinancity.core.captcha.imageTypers.ImageTypersProvider;
import com.kinancity.core.captcha.impl.LogCaptchaCollector;
import com.kinancity.core.captcha.twoCaptcha.TwoCaptchaProvider;
import com.kinancity.core.errors.ConfigurationException;
import com.kinancity.core.generator.AccountGenerator;
import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.proxy.ProxyManager;
import com.kinancity.core.proxy.ProxyRecycler;
import com.kinancity.core.proxy.ProxyTester;
import com.kinancity.core.proxy.bottleneck.ProxyCooldownBottleneck;
import com.kinancity.core.proxy.bottleneck.ProxyNoBottleneck;
import com.kinancity.core.proxy.impl.HttpProxy;
import com.kinancity.core.proxy.impl.NoProxy;
import com.kinancity.core.proxy.policies.NintendoTimeLimitPolicy;
import com.kinancity.core.proxy.policies.ProxyPolicy;
import com.kinancity.core.proxy.policies.TimeLimitPolicy;
import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.worker.callbacks.ResultLogger;

import lombok.Data;

@Data
public class Configuration {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CONFIG_FILE = "config.properties";

	private static Configuration instance;

	private String captchaKey;

	private String captchaProvider = "imageTypers";

	private int nbThreads = 3;

	private boolean forceMaxThread = false;

	private Bottleneck<ProxyInfo> bottleneck;

	private boolean useIpBottleNeck = true;

	private CaptchaQueue captchaQueue;

	private ProxyManager proxyManager;

	private ProxyPolicy proxyPolicy;

	private AccountGenerator accountGenerator;

	private String resultLogFilename = "result.csv";

	private ResultLogger resultLogger;

	private boolean initDone = false;

	private boolean skipProxyTest = false;

	private int maxRetry = 3;

	private int captchaMaxTotalTime = 600;

	private int captchaMaxParallelChallenges = 20;

	// If true, everything will be mocked
	private boolean dryRun = false;

	private int dumpResult = PtcSession.NEVER;

	public static Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
			instance.load(CONFIG_FILE);
		}

		return instance;
	}

	public void init() throws ConfigurationException {

		try {

			if (captchaQueue == null) {
				try {

					captchaQueue = new CaptchaQueue(new LogCaptchaCollector());

					if ((captchaKey == null || captchaKey.isEmpty()) && !"local".equals(captchaProvider)) {
						throw new ConfigurationException("No Catpcha key given");
					}
					
					try {
						CaptchaProvider provider = null;
						String providerThreadName = "";

						if ("2captcha".equals(captchaProvider)) {
						// Add 2 captcha Provider
							provider = TwoCaptchaProvider.getInstance(captchaQueue, captchaKey);
							providerThreadName = "2captcha";
						} else if ("imageTypers".equals(captchaProvider)) {
							// Add imageTypers Provider
							provider = ImageTypersProvider.getInstance(captchaQueue, captchaKey);
							providerThreadName = "imageTypers";
						} else if ("antiCaptcha".equals(captchaProvider)) {
							// Add imageTypers Provider
							provider = AntiCaptchaProvider.getInstance(captchaQueue, captchaKey);
							providerThreadName = "antiCaptcha";
						} else if ("local".equals(captchaProvider)) {
							// Add local server provider
							provider = ClientProvider.getInstance(captchaQueue);
							providerThreadName = "localCaptchaServer";
						} else {
							throw new ConfigurationException("Unknown captcha provider " + captchaProvider);
						}

						// Proceed running captcha thread

						provider.setMaxWait(captchaMaxTotalTime);
						provider.setMaxParallelChallenges(captchaMaxParallelChallenges);

						double balance = provider.getBalance();
						if (balance < 0) {
							logger.warn("WARNING !! : Current captcha balance is negative {}", balance);
						} else {
							logger.info("Catpcha Key is valid. Current captcha balance is {}", balance);
						}

						Thread captchaThread = new Thread(provider);
						captchaThread.setName(providerThreadName);
						captchaThread.start();

					} catch (CaptchaException e) {
						throw new ConfigurationException(e);
					}

				} catch (TechnicalException e) {
					throw new ConfigurationException(e);
				}
			}

			if (proxyManager == null) {
				logger.info("ProxyManager using direct connection with Nintendo policy");
				proxyManager = new ProxyManager();
				// Add Direct connection
				proxyManager.addProxy(new ProxyInfo(getProxyPolicyInstance(), new NoProxy()));
			}

			// Add BottleNeck
			if (bottleneck == null) {
				if (!useIpBottleNeck) {
					bottleneck = new ProxyNoBottleneck();
				} else {
					ProxyCooldownBottleneck ipBottleneck = new ProxyCooldownBottleneck();
					Thread bottleNeckThread = new Thread(ipBottleneck);
					bottleNeckThread.setName("OfficerJenny(BottleNeck)");
					bottleNeckThread.start();
					bottleneck = ipBottleneck;
				}
			}

			// Add Proxy recycler and start thread
			ProxyRecycler recycler = new ProxyRecycler(proxyManager);
			recycler.setBottleneck(bottleneck);
			Thread recyclerThread = new Thread(recycler);
			recyclerThread.setName("NurseJoy(Recycler)");
			recyclerThread.start();
			proxyManager.setRecycler(recycler);

			if (resultLogger == null) {
				resultLogger = new ResultLogger(new PrintWriter(new FileWriter(resultLogFilename, true)));
			}
		} catch (

		IOException e) {
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
				logger.error("Configuration Init Failed : " + e.getMessage());
				logger.debug("Stacktrace", e);
				return false;
			}
		}

		if (accountGenerator == null) {
			logger.error("Missing Account Generator");
			return false;
		}

		if (captchaQueue == null) {
			logger.error("Missing Captcha Queue");
			return false;
		}

		if (!skipProxyTest) {

			ProxyTester proxyTester = new ProxyTester();
			List<ProxyInfo> invalidProxies = new ArrayList<>();
			if (proxyManager.getProxies().isEmpty()) {
				logger.error("No valid proxy given");
				return false;
			}

			logger.info("Validating {} given proxies", proxyManager.getProxies().size());

			for (ProxyInfo proxy : proxyManager.getProxies()) {
				if (!proxyTester.testProxy(proxy.getProvider())) {
					logger.warn("Proxy test for {} failed, remove proxy", proxy.getProvider());
					invalidProxies.add(proxy);
				}
			}
			if (invalidProxies.isEmpty()) {
				logger.info("All proxies are valid");
			} else {
				proxyManager.getProxies().removeAll(invalidProxies);
				logger.info("{} valid proxies left", proxyManager.getProxies().size());
				if (proxyManager.getProxies().size() == 0) {
					return false;
				}
			}
		}

		if (forceMaxThread) {
			// Max 3 times more thread then proxies
			int maxThreads = proxyManager.getProxies().size() * 3;
			if (nbThreads > maxThreads) {
				nbThreads = maxThreads;
				logger.info("Too many thread compared to proxies, forcing thread count to {}", nbThreads);
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
			this.setCaptchaKey(prop.getProperty("captcha.key"));
			this.setCaptchaProvider(prop.getProperty("captcha.provider", "imageTypers"));
			this.setDumpResult(Integer.parseInt(prop.getProperty("dumpResult", String.valueOf(PtcSession.NEVER))));
			this.setCaptchaMaxTotalTime(Integer.parseInt(prop.getProperty("captchaMaxTotalTime", String.valueOf(captchaMaxTotalTime))));

			this.setCaptchaMaxParallelChallenges(Integer.parseInt(prop.getProperty("captchaMaxParallelChallenges", String.valueOf(captchaMaxParallelChallenges))));

			String customPeriod = prop.getProperty("proxyPolicy.custom.period");
			if (customPeriod != null && NumberUtils.isNumber(customPeriod)) {
				proxyPolicy = new TimeLimitPolicy(5, Integer.parseInt(customPeriod) * 60);
			}

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
				HttpProxy proxy = HttpProxy.fromURI(proxyDef.trim());
				if (proxy == null) {
					logger.error("Invalid proxy {}", proxyDef);
				} else {
					proxyManager.addProxy(new ProxyInfo(getProxyPolicyInstance(), proxy));
				}
			}

			logger.info("ProxyManager setup with {} proxies : ", proxyManager.getProxies().size());
			for (ProxyInfo proxy : proxyManager.getProxies()) {
				logger.info(" - {}", proxy.toString());
			}

		}

	}

	private ProxyPolicy getProxyPolicyInstance() {
		if (proxyPolicy == null) {
			proxyPolicy = new NintendoTimeLimitPolicy();
		}
		return proxyPolicy.clone();
	}

	/**
	 * Make sure to use the given policy.
	 */
	public void reloadProxyPolicy() {
		if (proxyManager != null) {
			ProxyPolicy policy = getProxyPolicyInstance();
			proxyManager.getProxies().stream().forEach(proxy -> proxy.setProxyPolicy(getProxyPolicyInstance()));
			logger.info("ProxyManager reloaded with {} policy ", policy);
		}

	}

}
