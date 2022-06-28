package com.kinancity.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import com.kinancity.mail.mailchanger.*;
import com.kinancity.mail.mailchanger.password.PasswordProvider;
import com.kinancity.mail.config.PasswordProviderFactory;
import com.kinancity.mail.wiser.KinanWiser;
import lombok.extern.slf4j.Slf4j;

import com.kinancity.mail.activator.LinkActivator;
import com.kinancity.mail.activator.MultiThreadQueueLinkActivator;
import com.kinancity.mail.activator.QueueLinkActivator;
import com.kinancity.mail.activator.ToFileLinkActivator;
import com.kinancity.mail.activator.limiter.RateLimiter;
import com.kinancity.mail.proxy.HttpProxy;
import com.kinancity.mail.tester.ThrottleTester;

@Slf4j
public class MailServerApplication {

	private static final String CONFIG_FILE = "config.properties";

	private static Properties config;

	public static void main(String[] args) {

		loadConfig();

		String mode = "direct";

		if (args.length > 0) {
			mode = args[0].toLowerCase();
		}

		if (mode.equals("test")) {
			log.info("Start in Tester Mode");

			ThrottleTester tester = new ThrottleTester();

			if (args.length > 1) {
				tester.setDelay(Integer.parseInt(args[1]));
			}

			if (args.length > 2) {
				tester.setErrorDelay(Integer.parseInt(args[2]));
			}

			if (args.length > 3) {
				tester.setPauseAfterNRequests(Integer.parseInt(args[3]));
			}

			if (args.length > 4) {
				tester.setDelayAfterNRequests(Integer.parseInt(args[4]));
			}

			tester.start();
		} else if (mode.equals("file")) {

			String filePath = "links.log";
			if (args.length > 1) {
				filePath = args[1];
			}

			LinkActivator activator = getQueueLinkActivator();
			EmailChanger mailChanger = getEmailChanger();

			HybridActivator hybrid = new HybridActivator(activator, mailChanger);
			hybrid.start();

			log.info("Started in file Mode");
			SkippedFileProcessor processor = new SkippedFileProcessor(hybrid, filePath);
			processor.process();

		} else {
			LinkActivator activator = getQueueLinkActivator();
			EmailChanger emailChanger;

			if (mode.equals("log")) {
				log.info("Started in Log Mode");
				activator = new ToFileLinkActivator();
				emailChanger = new ToFileEmailChanger();
			} else {
				log.info("Started in Direct Mode");
				emailChanger = getEmailChanger();
			}
			activator.start();

			// Start Wiser server
			KinanWiser wiser = new KinanWiser();
			int port = Integer.parseInt(config.getProperty("port", "25"));
			wiser.setPort(port);
			wiser.setHostname(config.getProperty("hostname", ""));

			// Additional Setup
			wiser.getServer().setSoftwareName("Kinan Mail Server");


			log.info("SMTP server started on port {}", port);

			String allowedDomains = config.getProperty("allowedDomains");
			if(allowedDomains != null) {
				log.info("Only accept emails for " + allowedDomains);
				wiser.setAllowedDomain(allowedDomains);
			}

			KcMessageHandlerFactory handlerFactory = new KcMessageHandlerFactory(activator, emailChanger);
			boolean disableDomainFilter = config.getProperty("disableDomainFilter", "false").equals("true");
			if (disableDomainFilter) {
				handlerFactory.setAcceptAllFrom(true);
			}

			if(activator != null ) {
				log.info("Email Activation is Enabled");
			}

			if(emailChanger != null) {
				log.info("Email Change is Enabled");
			}


			wiser.getServer().setMessageHandlerFactory(handlerFactory);
			wiser.start();
		}
	}

	public static void loadConfig() {
		config = new Properties();

		try {
			File configFile = new File(CONFIG_FILE);
			if (!configFile.exists()) {
				log.info("Missing configuration file " + CONFIG_FILE);
				return;
			}
			InputStream in = new FileInputStream(configFile);
			config.load(in);
			in.close();
		} catch (IOException e) {
			log.info("Error loading configuration file " + CONFIG_FILE);
		}

    if (config.getProperty("proxy") == null) {
      String proxyEnvConfig = System.getenv("proxy");
      if (proxyEnvConfig != null) {
        log.info("Use proxy config from SytemEnv variable : " + proxyEnvConfig);
        config.setProperty("proxy", proxyEnvConfig);
      }
    }

    

	}

	private static EmailChanger getEmailChanger() {
		String isActive = config.getProperty("emailChanger.active");
		if(isActive == null || isActive.equals("true")) {
			PasswordProvider passwordProvider = PasswordProviderFactory.getPasswordProvider(config);
			DirectEmailChanger emailChanger = new DirectEmailChanger(passwordProvider);

			String proxy = config.getProperty("proxy");
			if (proxy != null) {
				if (proxy.contains("|")) {
					List<String> proxies = new LinkedList<String>(Arrays.asList(proxy.split(Pattern.quote("|"))));
					String initialProxy = proxies.get(0);
					HttpProxy httpProxy = HttpProxy.fromURI(initialProxy);
					log.info("Mailchanger using proxy " + httpProxy);
					emailChanger.setHttpProxy(httpProxy);

					proxies.remove(0);
					for (String backupProxyStr : proxies) {
						HttpProxy backupProxy = HttpProxy.fromURI(backupProxyStr);
						httpProxy.getOtherProxies().add(backupProxy);
						log.info("Mailchanger with backup proxy " + backupProxy);
					}

				} else {
					HttpProxy httpProxy = HttpProxy.fromURI(proxy);
					log.info("Mailchanger using proxy " + httpProxy);
					emailChanger.setHttpProxy(httpProxy);
				}
			}
			return emailChanger;
		} else {
			return null;
		}
	}

	public static LinkActivator getQueueLinkActivator() {

		QueueLinkActivator activator;

		String multithreadActive = config.getProperty("multithread.enable");
		if (multithreadActive != null && !multithreadActive.equals("false")) {
			MultiThreadQueueLinkActivator mtactivator = new MultiThreadQueueLinkActivator();
			
			String multithreadNbThread = config.getProperty("multithread.nbThread");
			if(multithreadNbThread != null){
				mtactivator.setNbThreads(Integer.parseInt(multithreadNbThread));	
			}
			activator = mtactivator;
		} else {
			activator = new QueueLinkActivator();
		}

		String proxy = config.getProperty("proxy");
		if (proxy != null) {
			if (proxy.contains("|")) {
				List<String> proxies = new LinkedList<String>(Arrays.asList(proxy.split(Pattern.quote("|"))));
				String initialProxy = proxies.get(0);
				HttpProxy httpProxy = HttpProxy.fromURI(initialProxy);
				log.info("Activator using proxy " + httpProxy);
				activator.setHttpProxy(httpProxy);

				proxies.remove(0);
				for (String backupProxyStr : proxies) {
					HttpProxy backupProxy = HttpProxy.fromURI(backupProxyStr);
					httpProxy.getOtherProxies().add(backupProxy);
					log.info("Activator with backup proxy " + backupProxy);
				}

			} else {
				HttpProxy httpProxy = HttpProxy.fromURI(proxy);
				log.info("Activator using proxy " + httpProxy);
				activator.setHttpProxy(httpProxy);
			}
		}

		String limiterActive = config.getProperty("limiter.enable");
		if (limiterActive != null && !limiterActive.equals("false")) {
			String period = config.getProperty("limiter.period");
			String nb = config.getProperty("limiter.nb");
			String pause = config.getProperty("limiter.pause");

			RateLimiter limiter = new RateLimiter();
			if (period != null) {
				limiter.setPeriodInSeconds(Integer.parseInt(period));
			}
			if (nb != null) {
				limiter.setNbPerPeriod(Integer.parseInt(nb));
			}
			if (pause != null) {
				limiter.setLimiterPause(Integer.parseInt(pause));
			}

			log.info("Using limiter " + limiter);
			activator.setLimiter(limiter);
		}
		
		String proxyBanActive = config.getProperty("proxy.banProxyOn403");
		if (proxyBanActive != null && !proxyBanActive.equals("false")) {
			activator.setBanProxyOn403(true);
		}
		
		String alwaysSwitchActive = config.getProperty("proxy.switchProxyOnSuccess");
		if (alwaysSwitchActive != null && !alwaysSwitchActive.equals("false")) {
			activator.setSwitchProxyOnSuccess(true);
		}
		
		return activator;
	}

}
