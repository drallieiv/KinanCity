package com.kinancity.core.proxy.bottleneck;

import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.throttle.BottleneckCallback;
import com.kinancity.core.throttle.bottleneck.BottleneckSpaced;

/**
 * Will space calls by a given time
 * 
 * @author drallieiv
 *
 */
public class ProxySpacedBottleneck implements Runnable, Bottleneck<ProxyInfo> {

	private BottleneckSpaced<String> hostBottleNeck;

	public ProxySpacedBottleneck() {
		hostBottleNeck = new BottleneckSpaced<>();
	}

	public ProxySpacedBottleneck(int retentionTime) {
		hostBottleNeck = new BottleneckSpaced<>(retentionTime);
	}

	public void setRetentionTime(int retentionTime) {
		hostBottleNeck.setRetentionTime(retentionTime);
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
