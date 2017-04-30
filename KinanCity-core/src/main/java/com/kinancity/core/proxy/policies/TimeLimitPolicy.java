package com.kinancity.core.proxy.policies;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.kinancity.core.proxy.ProxySlot;

import lombok.Getter;

/**
 * Policy that limits to a number of account per period of time.
 * 
 * @author drallieiv
 *
 */
@Getter
public class TimeLimitPolicy implements ProxyPolicy {

	// History of calls
	public List<ProxySlot> slots;

	// How many account can we create during the time span
	public int maxPerPeriod;

	// Period with rolling time in seconds
	public long periodInSeconds;

	/**
	 * Constuctor for a Policy that limits to a number of account per period of time.
	 * 
	 * @param maxPerPeriod
	 *            number of accounts
	 * @param periodInSeconds
	 *            duration in seconds
	 */
	public TimeLimitPolicy(int maxPerPeriod, long periodInSeconds) {
		slots = new ArrayList<>();
		for (int i = 0; i < maxPerPeriod; i++) {
			slots.add(new ProxySlot());
		}
		this.maxPerPeriod = maxPerPeriod;
		this.periodInSeconds = periodInSeconds;
	}

	@Override
	public synchronized Optional<ProxySlot> getFreeSlot() {
		Optional<ProxySlot> freeSlot = slots.stream().filter(slot -> !slot.isReserved()).findFirst();
		if (freeSlot.isPresent()) {
			freeSlot.get().setReserved(true);
		}
		return freeSlot;
	}

	@Override
	public void markOverLimit() {
		slots.forEach(ProxySlot::markUsed);
	}

	@Override
	public boolean isAvailable() {
		// Should we do it each time, or scheduled ?
		cleanSlots();
		return slots.stream().filter(slot -> !slot.isReserved()).findFirst().isPresent();
	}

	// Remove calls over the time limit
	public void cleanSlots() {
		LocalDateTime cleanBefore = LocalDateTime.now().minusSeconds(periodInSeconds);
		slots.stream().filter(slot -> slot.isReserved() && slot.getLastUsed() != null && slot.getLastUsed().isBefore(cleanBefore)).forEach(ProxySlot::freeSlot);
	}

	public String toString() {
		return "max " + maxPerPeriod + " / " + Math.round(periodInSeconds / 60) + " min";
	}

	@Override
	public TimeLimitPolicy clone() {
		return new TimeLimitPolicy(this.getMaxPerPeriod(), this.getPeriodInSeconds());
	}

}
