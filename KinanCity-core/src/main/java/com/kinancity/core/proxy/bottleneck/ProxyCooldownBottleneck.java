package com.kinancity.core.proxy.bottleneck;

import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.throttle.BottleneckCallback;
import com.kinancity.core.throttle.bottleneck.BottleneckCooldown;

/**
 * Will space calls according to cooldown setup
 * 
 * @author drallieiv
 *
 */
public class ProxyCooldownBottleneck implements Runnable, Bottleneck<ProxyInfo> {

	private BottleneckCooldown<String> hostBottleNeck;

	public ProxyCooldownBottleneck() {
		hostBottleNeck = new BottleneckCooldown<>();
	}

	public ProxyCooldownBottleneck(int maxRequests, int pauseAfterMax) {
		hostBottleNeck = new BottleneckCooldown<>(maxRequests, pauseAfterMax);
	}

	@Override
	public void syncUseOf(ProxyInfo proxy) {
		hostBottleNeck.syncUseOf(proxy.getProvider().getHost());
	}

	@Override
	public void asyncUseOf(ProxyInfo proxy, BottleneckCallback callback) {
		hostBottleNeck.asyncUseOf(proxy.getProvider().getHost(), callback);
	}

	@Override
	public void run() {
		hostBottleNeck.run();
	}

	@Override
	public void onServerError(ProxyInfo proxy) {
		hostBottleNeck.onServerError(proxy.getProvider().getHost());
	}

}
