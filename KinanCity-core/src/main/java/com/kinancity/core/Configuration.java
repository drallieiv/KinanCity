package com.kinancity.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.kinancity.core.captcha.capsolver.CapsolverCaptchaProvider;
import com.kinancity.core.captcha.deathByCaptcha.DeathByCaptchaProvider;
import com.kinancity.core.captcha.twocaptchabasic.TwoCaptchaBasicProvider;
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

	public static final String PROVIDER_2CAPTCHA = "2captcha";
	public static final String PROVIDER_2CAPTCHA_BASIC = "2captchaBasic";
	public static final String PROVIDER_CAPTCHAAI = "captchaai";
	public static final String PROVIDER_IMAGETYPERS = "imageTypers";
	public static final String PROVIDER_ANTICAPTCHA = "antiCaptcha";

	public static final String PROVIDER_CAPSOLVER = "capsolver";

	public static final String PROVIDER_DBC = "deathbycaptcha";

	public static final String PROVIDER_LOCAL = "local";

	private Logger logger = LoggerFactory.getLogger(getClass());

	private static final String CONFIG_FILE = "config.properties";

	private static Configuration instance;

	private String captchaKey;

	private String captchaProvider = PROVIDER_2CAPTCHA;

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

	private boolean emailOptIn = false;

	private String twocaptchaAltHost = null;

	private int dumpResult = PtcSession.NEVER;

	private boolean debugCaptchaQueue = false;

	/**
	 * Custom values for batches processing
	 */
	private Integer customBatchMinTimeForRecovery = null;
	private Integer customBatchMissmatchRecoverySize = null;
	private Integer customBatchNormalBatchSize = null;

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

					if(debugCaptchaQueue || System.getenv("DEBUG_CAPTCHA") != null) {
						logger.info("Add Detailed Logs for Captcha Queue");
						captchaQueue.setAddDetailedLogs(true);
					}

					if ((captchaKey == null || captchaKey.isEmpty()) && !"local".equals(captchaProvider)) {
						throw new ConfigurationException("No Catpcha key given");
					}

					try {
						CaptchaProvider provider = null;
						String providerThreadName = "";

						if (PROVIDER_2CAPTCHA.equals(captchaProvider)) {
							// Add 2 captcha Provider
							TwoCaptchaProvider twocaptchaprovider = (TwoCaptchaProvider) TwoCaptchaProvider.getInstance(captchaQueue, captchaKey);
							if (customBatchMinTimeForRecovery != null) {
								twocaptchaprovider.setMinTimeForRecovery(customBatchMinTimeForRecovery);
							}
							if (customBatchMissmatchRecoverySize != null) {
								twocaptchaprovider.setMissmatchRecoverySize(customBatchMissmatchRecoverySize);
							}
							if (customBatchNormalBatchSize != null) {
								twocaptchaprovider.setNormalBatchSize(customBatchNormalBatchSize);
							}

							provider = twocaptchaprovider;
							providerThreadName = PROVIDER_2CAPTCHA;
						} else if (PROVIDER_IMAGETYPERS.equals(captchaProvider)) {
							// Add imageTypers Provider
							provider = ImageTypersProvider.getInstance(captchaQueue, captchaKey);
							providerThreadName = PROVIDER_IMAGETYPERS;
						} else if (PROVIDER_ANTICAPTCHA.equals(captchaProvider)) {
							// Add imageTypers Provider
							provider = AntiCaptchaProvider.getInstance(captchaQueue, captchaKey);
							providerThreadName = PROVIDER_ANTICAPTCHA;
						} else if (PROVIDER_LOCAL.equals(captchaProvider)) {
							// Add local server provider
							provider = ClientProvider.getInstance(captchaQueue);
							providerThreadName = "localCaptchaServer";
						} else if (PROVIDER_CAPSOLVER.equals(captchaProvider) ) {
							// Add imageTypers Provider
							provider = CapsolverCaptchaProvider.getInstance(captchaQueue, captchaKey);
							providerThreadName = PROVIDER_CAPSOLVER;
						} else if (PROVIDER_DBC.equals(captchaProvider)) {
							// Add imageTypers Provider
							provider = DeathByCaptchaProvider.getInstance(captchaQueue, captchaKey);
							providerThreadName = "DeathByCaptcha";
						} else if (PROVIDER_2CAPTCHA_BASIC.equals(captchaProvider)) {
							// Add 2captcha Basic Provider
							provider = TwoCaptchaBasicProvider.getInstance(captchaQueue, captchaKey, Optional.ofNullable(twocaptchaAltHost));
							providerThreadName = "2CaptchaBasic";
						} else if (PROVIDER_CAPTCHAAI.equals(captchaProvider)) {
							// Add CaptchaAi Provider as 2captcha basic with custom host
							provider = TwoCaptchaBasicProvider.getInstance(captchaQueue, captchaKey, Optional.of("ocr.captchaai.com"));
							providerThreadName = "2CaptchaBasic_CaptchaAI";
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
				ProxyPolicy policy = getProxyPolicyInstance();
				logger.info("ProxyManager using direct connection with policy : {}", policy);
				proxyManager = new ProxyManager();
				// Add Direct connection
				proxyManager.addProxy(new ProxyInfo(policy, new NoProxy()));
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
			this.setCaptchaProvider(prop.getProperty("captcha.provider", PROVIDER_2CAPTCHA));
			this.setDumpResult(Integer.parseInt(prop.getProperty("dumpResult", String.valueOf(PtcSession.NEVER))));
			this.setCaptchaMaxTotalTime(Integer.parseInt(prop.getProperty("captchaMaxTotalTime", String.valueOf(captchaMaxTotalTime))));
			Optional.ofNullable(prop.getProperty("batch.recovery.time")).ifPresent(value -> this.setCustomBatchMinTimeForRecovery(Integer.parseInt(value)));
			Optional.ofNullable(prop.getProperty("batch.recovery.size")).ifPresent(value -> this.setCustomBatchMissmatchRecoverySize(Integer.parseInt(value)));
			Optional.ofNullable(prop.getProperty("batch.normal.size")).ifPresent(value -> this.setCustomBatchNormalBatchSize(Integer.parseInt(value)));
			Optional.ofNullable(prop.getProperty("2captcha.altHost")).ifPresent(this::setTwocaptchaAltHost);

			this.setCaptchaMaxParallelChallenges(Integer.parseInt(prop.getProperty("captchaMaxParallelChallenges", String.valueOf(captchaMaxParallelChallenges))));

			if(prop.getProperty("proxyPolicy.custom.period") != null || prop.getProperty("proxyPolicy.custom.count") != null){
				String customPeriodConfig = prop.getProperty("proxyPolicy.custom.period");
				String customCountConfig = prop.getProperty("proxyPolicy.custom.count");
				int customPeriod = 16 * 60;
				int customCount = 5;
				if (customPeriodConfig != null && NumberUtils.isNumber(customPeriodConfig)) {
					customPeriod = Integer.parseInt(customPeriodConfig);
				}
				if (customCountConfig != null && NumberUtils.isNumber(customCountConfig)) {
					customCount = Integer.parseInt(customCountConfig);
				}

				proxyPolicy = new TimeLimitPolicy(customCount, customPeriod);
			}

			this.loadProxies(prop.getProperty("proxies"));

			this.setEmailOptIn(prop.getProperty("option.emailoptin","false").equals("true"));

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
			proxyManager.getProxies().forEach(proxy -> proxy.setProxyPolicy(getProxyPolicyInstance()));
			logger.info("ProxyManager reloaded with {} policy ", policy);
		}

	}

}
