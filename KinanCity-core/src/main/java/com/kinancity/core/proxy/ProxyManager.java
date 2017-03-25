package com.kinancity.core.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import lombok.Getter;

/**
 * Manager that holds a set of proxies
 * 
 * TODO : implement better proxy rotation
 * 
 * @author drallieiv
 *
 */
public class ProxyManager {

	@Getter
	// How much time should we retry getting a proxy if none are found. in ms
	private long pollingRate = 30000;
	
	@Getter
	// List of possible proxy instances
	private List<ProxyInfo> proxies;

	public ProxyManager() {
		proxies = new ArrayList<>();
	}
	
	private boolean proxyRotation = true;

	/**
	 * Add a new proxy possibility
	 * @param proxy
	 */
	public void addProxy(ProxyInfo proxy){
		this.proxies.add(proxy);
	}

	// Gives a proxy that can be used
	public synchronized Optional<ProxyInfo> getEligibleProxy(){
		Optional<ProxyInfo> proxyInfoResult = proxies.stream().filter( pi -> pi.isAvailable()).findFirst();
		ProxyInfo proxyInfo = null;
		if(proxyInfoResult.isPresent()){
			proxyInfo = proxyInfoResult.get();
			proxyInfo.getProxyPolicy().markUsed();
			
			// Reorder the proxy list to start after the found one
			if(proxyRotation){
				Collections.rotate(proxies, - proxies.indexOf(proxyInfo) + 1);
			}
		}
		return Optional.ofNullable(proxyInfo);
	}
}
