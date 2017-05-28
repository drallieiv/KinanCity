package com.kinancity.core.throttle.bottleneck;

import java.time.LocalDateTime;
import java.util.Map.Entry;

import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.throttle.WaitTicket;
import com.kinancity.core.throttle.queue.WaitQueueSpaced;

import lombok.Setter;

/**
 * Bottleneck that handles a queue of requests with fixed spacing between calls
 * 
 * @author drallieiv
 *
 */
public class BottleneckSpaced<R> extends BottleneckWithQueues<R, WaitQueueSpaced<R>> implements Runnable, Bottleneck<R> {

	// Dont call this IP if it was used in the last retentionTime seconds (default 11s which makes 6 requests in 66s > 60s)
	@Setter
	private int retentionTime = 11;

	private long runLoopPause = 500;

	public BottleneckSpaced() {
		
	}
	
	public BottleneckSpaced(int retentionTime) {
		this.retentionTime = retentionTime;
	}

	/**
	 * Will check every 'runLoopPause' if last use of the ressource was more then 'retentionTime' ago
	 */
	@Override
	public void run() {
		while (true) {

			LocalDateTime now = LocalDateTime.now();
			LocalDateTime minTime = now.minusSeconds(retentionTime);

			for (Entry<R, WaitQueueSpaced<R>> entry : ressourceQueueMap.entrySet()) {
				WaitQueueSpaced<R> waitingQueue = entry.getValue();

				if (!waitingQueue.getQueue().isEmpty()) {

					if (waitingQueue.getLastUse() == null || waitingQueue.getLastUse().isBefore(minTime)) {
						waitingQueue.setLastUse(now);
						WaitTicket<R> firstElement = waitingQueue.getQueue().pop();
						firstElement.clear();
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
	WaitQueueSpaced<R> newWaitQueue() {
		return new WaitQueueSpaced();
	}

}
