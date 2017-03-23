package com.kinancity.core.proxy.policies;

import lombok.Getter;

/**
 * Kind of dump policy that makes that
 * 
 * @author drallieiv
 *
 */
@Getter
public class MaxUsePolicy implements ProxyPolicy {

	public int maxUses = 5;

	// Current counter of number of uses
	public int nbUses = 0;

	public MaxUsePolicy(int maxUses) {
		this.maxUses = maxUses;
	}

	@Override
	public synchronized void markUsed() {
		nbUses++;
	}
	
	@Override
	public synchronized void freeOneTry() {
		nbUses--;
	}

	@Override
	public void markOverLimit() {
		nbUses = maxUses;
	}

	@Override
	public boolean isAvailable() {
		return nbUses < maxUses;
	}
	
	public String toString(){
		return maxUses+" Max";
	}

	@Override
	public MaxUsePolicy clone() {
		return new MaxUsePolicy(this.getMaxUses());
	}


}
