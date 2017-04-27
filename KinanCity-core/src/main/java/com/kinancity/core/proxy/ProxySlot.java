package com.kinancity.core.proxy;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * Represent one slot on a proxy
 * @author drallieiv
 *
 */
@Data
public class ProxySlot {
	
	private ProxyInfo info;
	
	private boolean reserved = false;
	
	private LocalDateTime lastUsed;
	
	/**
	 * Free the slot
	 */
	public void freeSlot(){
		lastUsed = null;
		reserved = false;
	}
	
	/**
	 * This slot has been used once
	 */
	public void markUsed(){
		lastUsed = LocalDateTime.now();
	}
}
