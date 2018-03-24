package com.kinancity.mail.activator.limiter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Setter;

@Setter
public class RateLimiter implements ActivationLimiter {

	// How long is the period
	private int periodInSeconds = 60;

	// How many max during perdio
	private int nbPerPeriod = 20;

	// Loop wait in seconds
	private int limiterPause = 1;

	// List of previous calls
	private List<LocalDateTime> calls = new ArrayList<>();

	@Override
	public void waitIfNecessary() {

		// Remove older calls
		cleanup();

		while (calls.size() >= nbPerPeriod) {
			try {
				Thread.sleep(limiterPause * 1000);
			} catch (InterruptedException e) {
				// Sleep interrupted
			}
			cleanup();
		}

		calls.add(LocalDateTime.now());
	}

	public void cleanup() {
		LocalDateTime cleanDate = LocalDateTime.now().minus(periodInSeconds, ChronoUnit.SECONDS);
		calls = calls.stream().filter(date -> date.isBefore(cleanDate)).collect(Collectors.toList());
	}

	@Override
	public String toString() {
		return "RateLimiter [periodInSeconds=" + periodInSeconds + ", nbPerPeriod=" + nbPerPeriod + ", limiterPause=" + limiterPause + "]";
	}

	
}
