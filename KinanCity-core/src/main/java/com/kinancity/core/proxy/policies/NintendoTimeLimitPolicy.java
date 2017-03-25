package com.kinancity.core.proxy.policies;

import lombok.Getter;

/**
 * Policy that limits to 5 accounts every 10 minutes.
 * 
 * @author drallieiv
 *
 */
@Getter
public class NintendoTimeLimitPolicy extends TimeLimitPolicy {

	// How many account can we create during the time span
	public static final int NINTENDO_MAX_NB = 5;

	// Period with a 15s safety
	public static final long NINTENDO_MAX_TIME = 10 * 60 + 15;

	public NintendoTimeLimitPolicy() {
		super(NINTENDO_MAX_NB, NINTENDO_MAX_TIME);
	}

	public String toString() {
		return super.toString();
	}

	@Override
	public NintendoTimeLimitPolicy clone() {
		return new NintendoTimeLimitPolicy();
	}

}
