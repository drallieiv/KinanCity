package com.kinancity.core.captcha;

import com.kinancity.api.errors.TechnicalException;

import lombok.Getter;
import lombok.Setter;

public abstract class CaptchaProvider implements Runnable {

	/**
	 * How much should we wait for each captcha in seconds. (default 600s, 0 for infinite)
	 */
	@Setter
	@Getter
	private int maxWait = 600;

	/**
	 * max number of captcha really waiting on 2captcha side
	 */
	@Setter
	@Getter
	private int maxParallelChallenges = 20;

	public abstract double getBalance() throws CaptchaException, TechnicalException;

}
