package com.kinancity.core.throttle;

public interface Bottleneck<T> {

	// Will wait for the resource to be usable and sleep meanwhile
	void syncUseOf(T resource);

	// Non blocking call, but the callback will be called once the resource is usable
	void asyncUseOf(T resource, BottleneckCallback callback);
	
	// Called when a 503 occurs
	void onServerError(T resource);
}