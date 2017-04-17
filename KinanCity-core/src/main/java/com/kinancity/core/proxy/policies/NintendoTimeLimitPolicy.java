package com.kinancity.core.proxy.policies;

import lombok.Getter;

/**
 * Policy that limits to 5 accounts every 15 minutes.
 * 
 * @author drallieiv
 *
 */
@Getter
public class NintendoTimeLimitPolicy extends TimeLimitPolicy {

	// How many account can we create during the time span
	public static final int NINTENDO_MAX_NB = 5;

	// Period with a 60s safety
	public static final long NINTENDO_MAX_TIME = 15 * 60 + 60;

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
