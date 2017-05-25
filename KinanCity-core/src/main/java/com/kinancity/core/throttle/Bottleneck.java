package com.kinancity.core.throttle;

import com.kinancity.core.proxy.ProxyInfo;

public interface Bottleneck {

	// Waits until
	void syncUseOf(ProxyInfo proxy);

}