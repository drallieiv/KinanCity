package com.kinancity.core.throttle.bottleneck;

import java.time.LocalDateTime;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.throttle.WaitTicket;
import com.kinancity.core.throttle.queue.WaitQueueCooldown;

import lombok.Setter;

/**
 * Bottleneck that handles a queue of requests with fixed spacing between calls
 * 
 * @author drallieiv
 *
 */
public class BottleneckCooldown<R> extends BottleneckWithQueues<R, WaitQueueCooldown<R>> implements Runnable, Bottleneck<R> {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Setter
	private int maxRequests = 6;

	@Setter
	private int pauseAfterMax = 65; // seconds

	private long runLoopPause = 500;

	public BottleneckCooldown() {

	}

	public BottleneckCooldown(int maxRequests, int pauseAfterMax) {
		this.maxRequests = maxRequests;
		this.pauseAfterMax = pauseAfterMax;
	}

	/**
	 * Will check every 'runLoopPause' if last use of the resource was more then 'retentionTime' ago
	 */
	@Override
	public void run() {
		logger.info("Starting Bottleneck with a cooldown of {}s every {} resources access", pauseAfterMax, maxRequests);
		while (true) {

			for (Entry<R, WaitQueueCooldown<R>> entry : ressourceQueueMap.entrySet()) {
				WaitQueueCooldown<R> waitingQueue = entry.getValue();

				while (!waitingQueue.getQueue().isEmpty() && waitingQueue.getCount() < maxRequests) {
					WaitTicket<R> firstElement = waitingQueue.getQueue().pop();
					firstElement.clear();
					waitingQueue.countOne();
				}

				if (!waitingQueue.getQueue().isEmpty() && waitingQueue.getCount() >= maxRequests) {
					if (waitingQueue.getBurnOutReached() == null) {
						logger.debug("Burn waitingQueue for {} for {}s", entry.getKey(), pauseAfterMax);
						waitingQueue.setBurnOutReached(LocalDateTime.now());
					} else {
						LocalDateTime releaseTime = waitingQueue.getBurnOutReached().plusSeconds(pauseAfterMax);
						if (LocalDateTime.now().isAfter(releaseTime)) {
							logger.debug("Reset waitingQueue for {}", entry.getKey());
							waitingQueue.setCount(0);
							waitingQueue.setBurnOutReached(null);
						} else {
							// logger.debug("WaitingQueue for {} is on cooldown", entry.getKey());
						}
					}
				}
			}

			try {
				Thread.sleep(runLoopPause);
			} catch (InterruptedException e) {
				// Interrupted
			}
		}
	}

	@Override
	WaitQueueCooldown<R> newWaitQueue() {
		return new WaitQueueCooldown<R>();
	}

	@Override
	public void onServerError(R resource) {
		WaitQueueCooldown<R> ressource = ressourceQueueMap.get(resource);
		if (ressource != null) {
			ressource.setBurnOutReached(LocalDateTime.now());
		}
	}

}
