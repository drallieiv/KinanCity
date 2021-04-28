package com.kinancity.core.throttle.bottleneck;

import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.throttle.BottleneckCallback;

public class NoBottleneck<T> implements Bottleneck<T> {

	@Override
	public void syncUseOf(T proxy) {
		// DO nothing		
	}

	@Override
	public void asyncUseOf(T ressource, BottleneckCallback callback) {
		// Directly call the callback
		callback.onBottleneckPass();
	}

	@Override
	public void onServerError(T resource) {
		// DO nothing
	}

}
