package com.kinancity.core.proxy.policies;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Policy that limits to a number of account per period of time.
 * 
 * @author drallieiv
 *
 */
@Getter
public class TimeLimitPolicy implements ProxyPolicy {

	// History of calls
	public List<LocalDateTime> lastCalls;

	// How many account can we create during the time span
	public int maxPerPeriod;

	// Period with rolling time in seconds
	public long periodInSeconds;

	public TimeLimitPolicy(int maxPerPeriod, long periodInSeconds) {
		lastCalls = new ArrayList<>();
		this.maxPerPeriod = maxPerPeriod;
		this.periodInSeconds = periodInSeconds;
	}
	
	@Override
	public void markUsed() {
		lastCalls.add(LocalDateTime.now());
	}

	@Override
	public void markOverLimit() {
		// Clear the whole list and replace by all now
		lastCalls.clear();
		while (lastCalls.size() < maxPerPeriod) {
			lastCalls.add(LocalDateTime.now());
		}
	}

	@Override
	public boolean isAvailable() {
		// Should we do it each time, or scheduled ?
		cleanOlders();
		return lastCalls.size() < maxPerPeriod;
	}

	// Remove calls over the time limit
	public void cleanOlders() {
		LocalDateTime cleanBefore = LocalDateTime.now().minusSeconds(periodInSeconds);
		lastCalls.removeIf(call -> call.isBefore(cleanBefore));
	}

	public String toString() {
		return lastCalls.size() + "/" + maxPerPeriod + " over " + periodInSeconds + " seconds";
	}
}
