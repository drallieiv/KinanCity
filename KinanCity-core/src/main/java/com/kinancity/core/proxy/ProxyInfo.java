package com.kinancity.core.proxy;

import com.kinancity.core.proxy.policies.ProxyPolicy;

import lombok.Getter;

/**
 * Proxy information
 * @author drallieiv
 *
 */
@Getter
public class ProxyInfo {

	/**
	 * Policy that manages how much it is used and if we can use it now 
	 */
	private ProxyPolicy proxyPolicy;
	
	/**
	 * Provider of the HTTP client
	 */
	private HttpProxyProvider provider;

	public ProxyInfo(ProxyPolicy proxyPolicy, HttpProxyProvider provider) {
		this.proxyPolicy = proxyPolicy;
		this.provider = provider;
	}
	
	public boolean isAvailable(){
		return proxyPolicy.isAvailable();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(provider);
		sb.append(" : ");
		sb.append(proxyPolicy);
		return sb.toString();
	}

	public void freeOneTry() {
		proxyPolicy.freeOneTry();
	}
		
}
