package com.kinancity.core.throttle.bottleneck;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BottleneckCooldownTest implements Runnable {

	private Logger logger = LoggerFactory.getLogger(getClass());

	private List<String> proxies;

	private int nbRun = 30;

	private int nbThread = 3;
	
	private int nbProxies = 3;

	private BottleneckCooldown<String> bottleneck;

	@Test
	public void singleProxyTest() throws InterruptedException {

		bottleneck = new BottleneckCooldown<>(6, 10);
		Thread bottleNeckThread = new Thread(bottleneck);
		bottleNeckThread.start();

		proxies = new ArrayList<>();
		for (int i = 0; i < nbProxies; i++) {
			proxies.add("proxy" + i);
		}

		List<Thread> threads = new ArrayList<>();

		for (int i = 0; i < nbThread; i++) {
			Thread testThread = new Thread(this);
			threads.add(testThread);
			testThread.start();
		}

		while (threads.stream().filter(t -> t.isAlive()).count() != 0) {
			Thread.sleep(500);
		}

	}

	@Override
	public void run() {
		for (int i = 1; i <= nbRun; i++) {
			int rint = (int) (Math.random() * proxies.size());
			String proxy = proxies.get(rint);
			bottleneck.syncUseOf(proxy);
			logger.info("{} Use of proxy {}", i, rint + 1);
		}
	}

}
