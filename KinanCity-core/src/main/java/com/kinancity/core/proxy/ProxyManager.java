package com.kinancity.core.proxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

/**
 * Manager that holds a set of proxies
 * 
 * TODO : implement better proxy rotation
 * 
 * @author drallieiv
 *
 */
public class ProxyManager {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Getter
	// How much time should we retry getting a proxy if none are found. in ms
	private long pollingRate = 30000;

	@Getter
	// List of possible proxy instances
	private List<ProxyInfo> proxies = new ArrayList<>();

	/**
	 * Move here the proxies which failed
	 */
	@Getter
	private List<ProxyInfo> proxyBench = new ArrayList<>();

	@Setter
	@Getter
	private ProxyRecycler recycler;
	
	private boolean proxyRotation = true;

	/**
	 * Add a new proxy possibility
	 * 
	 * @param proxy
	 */
	public void addProxy(ProxyInfo proxy) {
		this.proxies.add(proxy);
	}

	// Gives a proxy that can be used
	public synchronized Optional<ProxySlot> getEligibleProxy() {
		Optional<ProxyInfo> proxyInfoResult = proxies.stream().filter(pi -> pi.isAvailable()).findFirst();
		ProxyInfo proxyInfo = null;
		if (proxyInfoResult.isPresent()) {
			proxyInfo = proxyInfoResult.get();
			Optional<ProxySlot> slot = proxyInfo.getFreeSlot();

			// Reorder the proxy list to start after the found one
			if (proxyRotation) {
				Collections.rotate(proxies, -proxies.indexOf(proxyInfo) + 1);
			}

			return slot;
		}
		return Optional.empty();
	}

	public void benchProxy(ProxyInfo proxy) {
		proxies.remove(proxy);
		proxyBench.add(proxy);
		logger.warn("Proxy [{}] moved out of rotation, {} proxy left",proxy, proxies.size());
		
		if(recycler != null && getNbProxyInRotation() == 0){
			recycler.setFastMode(true);
			recycler.checkAndRecycleAllBenched();
		}
		
	}
	
	public int getNbProxyInRotation(){
		return proxies.size();
	}
	
	public int getNbProxyBenched(){
		return proxyBench.size();
	}
}
