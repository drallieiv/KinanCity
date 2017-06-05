package com.kinancity.core.proxy.policies;

import java.util.Optional;

import com.kinancity.core.proxy.ProxySlot;

import lombok.Getter;

/**
 * Ideal unlimited use policy.
 * 
 * Never gets blocked. Always available.
 * 
 * @author drallieiv
 *
 */
@Getter
public class UnlimitedUsePolicy implements ProxyPolicy {

	@Override
	public boolean isAvailable() {
		return true;
	}
    
	@Override
	public void markOverLimit() {
        	return;
	}
    
	@Override
	public UnlimitedUsePolicy clone() {
		return new UnlimitedUsePolicy();
	}

	@Override
	public Optional<ProxySlot> getFreeSlot() {
		return Optional.of(new ProxySlot());
	}
	
	public String toString() {
		return "unlimited";
	}
	
}
