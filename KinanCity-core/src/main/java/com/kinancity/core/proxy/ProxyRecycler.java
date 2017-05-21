package com.kinancity.core.proxy;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * Works with a ProxyManager to automatically recycle bench out proxies
 * 
 * @author drallieiv
 *
 */
public class ProxyRecycler implements Runnable {

	private Logger logger = LoggerFactory.getLogger(getClass());

	// Pass every 5 minutes
	@Getter
	@Setter
	private int wait = 5 * 60 * 1000;
	
	// Pass every 1 minutes
	@Getter
	@Setter
	private int fastwait = 1 * 60 * 1000;

	@Setter
	private boolean runFlag = true;
	
	// mode that makes retry faster until one proxy gets back up
	@Setter
	private boolean fastMode = false;

	private ProxyManager proxyManager;

	@Getter
	@Setter
	private ProxyTester tester;

	public ProxyRecycler(ProxyManager proxyManager) {
		this.proxyManager = proxyManager;
		this.tester = new ProxyTester();
	}

	@Override
	public void run() {
		logger.info("Benched proxy recycler started.");
		while (runFlag) {
			// Sleep First
			try {
				Thread.sleep(fastMode ? fastwait : wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			checkAndRecycleAllBenched();
		}
	}

	public synchronized void checkAndRecycleAllBenched() {
		List<ProxyInfo> benchedProxies = proxyManager.getProxyBench();
		if (benchedProxies.isEmpty()) {
			logger.debug("No benched proxies");
		} else {
			logger.info("Check and recycle {} benched proxies", benchedProxies.size());
			ArrayList<ProxyInfo> proxiesToCheck = new ArrayList<>();
			proxiesToCheck.addAll(benchedProxies);
			proxiesToCheck.stream().forEach(this::checkAndRecycle);
		}
	}

	public synchronized void checkAndRecycle(ProxyInfo proxy) {
		if (proxyManager.getProxyBench().contains(proxy)) {
			boolean valid = tester.testProxy(proxy.getProvider());
			if (valid) {
				logger.info("Proxy [{}] is working again, set it back in rotation", proxy.getProvider());
				fastMode = false;
				// move proxy from bench back in rotation
				proxyManager.getProxyBench().remove(proxy);
				proxyManager.getProxies().add(proxy);
			}
		} else {
			logger.debug("Proxy is not benched, or already removed from bench");
		}

	}

}
