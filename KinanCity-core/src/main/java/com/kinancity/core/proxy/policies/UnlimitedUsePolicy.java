package com.kinancity.core.proxy.policies;

import lombok.Getter;

/**
 * Ideal unlimited use policy.
 * 
 * Only gets blocked if marked over limit Can be released with reset
 * 
 * @author drallieiv
 *
 */
@Getter
public class UnlimitedUsePolicy implements ProxyPolicy {

	private boolean overLimit = false;

	public void reset() {
		overLimit = false;
	}

	@Override
	public void markUsed() {
		// Nothing to do
	}
	
	@Override
	public void freeOneTry() {
		// Nothing to do	
	}

	@Override
	public void markOverLimit() {
		overLimit = true;
	}

	@Override
	public boolean isAvailable() {
		return !overLimit;
	}

	public String toString() {
		return "unlimited";
	}

	@Override
	public UnlimitedUsePolicy clone() {
		return new UnlimitedUsePolicy();
	}


}
