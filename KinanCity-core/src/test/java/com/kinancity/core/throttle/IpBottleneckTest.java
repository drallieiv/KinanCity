package com.kinancity.core.throttle;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.proxy.impl.HttpProxy;
import com.kinancity.core.proxy.policies.NintendoTimeLimitPolicy;
import com.kinancity.core.proxy.policies.ProxyPolicy;

public class IpBottleneckTest {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Test
	public void singleProxyTest() {

		IpBottleneck bottleneck = new IpBottleneck();
		
		Thread bottleNeckThread = new Thread(bottleneck);
		bottleNeckThread.start();

		ProxyPolicy proxyPolicy = new NintendoTimeLimitPolicy();
		ProxyInfo proxy1 = new ProxyInfo(proxyPolicy, new HttpProxy("test", 1234));
		
		logger.info("Start using proxy 1");
		
		bottleneck.syncUseOf(proxy1);
		logger.info("First Use of proxy 1");
		bottleneck.syncUseOf(proxy1);
		logger.info("Second Use of proxy 1");
		bottleneck.syncUseOf(proxy1);
		logger.info("Third Use of proxy 1");
	}

	@Test
	public void parallelUsesTest() {

		IpBottleneck bottleneck = new IpBottleneck();
		
		Thread bottleNeckThread = new Thread(bottleneck);
		bottleNeckThread.start();

		ProxyPolicy proxyPolicy = new NintendoTimeLimitPolicy();
		ProxyInfo proxy1 = new ProxyInfo(proxyPolicy, new HttpProxy("test", 1234));
		ProxyInfo proxy2 = new ProxyInfo(proxyPolicy, new HttpProxy("test2", 1234));
		
		logger.info("Start using proxy 1");		
		bottleneck.syncUseOf(proxy1);
		logger.info("First Use of proxy 1");
		
		bottleneck.syncUseOf(proxy2);
		logger.info("First Use of proxy 2");
			
		bottleneck.syncUseOf(proxy2);
		logger.info("Second Use of proxy 2");
		bottleneck.syncUseOf(proxy1);
		logger.info("Second Use of proxy 1");
		bottleneck.syncUseOf(proxy1);
			
		bottleneck.syncUseOf(proxy2);
		logger.info("Third Use of proxy 2");
	}
	
}
