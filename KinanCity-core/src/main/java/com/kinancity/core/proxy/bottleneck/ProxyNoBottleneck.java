package com.kinancity.core.proxy.bottleneck;

import com.kinancity.core.proxy.ProxyInfo;
import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.throttle.BottleneckCallback;
import com.kinancity.core.throttle.bottleneck.NoBottleneck;

public class ProxyNoBottleneck implements Bottleneck<ProxyInfo> {

	private NoBottleneck<String> hostBottleNeck;

	public ProxyNoBottleneck() {
		hostBottleNeck = new NoBottleneck<>();
	}

	@Override
	public void syncUseOf(ProxyInfo proxy) {
		hostBottleNeck.syncUseOf(proxy.getProvider().getHost());
	}

	@Override
	public void asyncUseOf(ProxyInfo proxy, BottleneckCallback callback) {
		hostBottleNeck.asyncUseOf(proxy.getProvider().getHost(), callback);
	}

}
