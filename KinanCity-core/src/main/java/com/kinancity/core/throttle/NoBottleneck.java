package com.kinancity.core.throttle;

import com.kinancity.core.proxy.ProxyInfo;

public class NoBottleneck implements Bottleneck {

	@Override
	public void syncUseOf(ProxyInfo proxy) {
		// DO nothing		
	}

}
