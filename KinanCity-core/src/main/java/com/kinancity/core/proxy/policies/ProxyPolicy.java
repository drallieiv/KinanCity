package com.kinancity.core.proxy.policies;

/**
 * Interface that manages if that proxy can be used right now
 * @author drallieiv
 *
 */
public interface ProxyPolicy extends Cloneable {
	
	// Triggered each time the proxy is used to get a HttpClient
	void markUsed();
	
	// Triggerd if the PTC server responded that we are over limit
	void markOverLimit();
	
	boolean isAvailable();
	
	ProxyPolicy clone();

	void freeOneTry();
}
