package com.kinancity.core.throttle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.kinancity.core.proxy.ProxyInfo;

import lombok.Setter;

/**
 * Restrict usage of the same IP
 * 
 * @author drallieiv
 *
 */
public class IpBottleneck implements Runnable, Bottleneck {

	// Dont call this IP if it was used in the last retentionTime seconds (default 15)
	@Setter
	private int retentionTime = 15;

	private Map<String, IpWaitQueue> hostQueues = new HashMap<>();

	private long runLoopPause = 500;
	
	public IpBottleneck() {
		
	}
	
	public IpBottleneck(int retentionTime) {
		this.retentionTime = retentionTime;
	}

	// Waits until proxy has not been used for a while
	public synchronized void syncUseOf(ProxyInfo proxy) {
		String host = proxy.getProvider().getHost();
		IpWaitTicket ticket = new IpWaitTicket(host);
		IpWaitQueue queue = hostQueues.get(host);

		if (queue == null) {
			queue = new IpWaitQueue();
			hostQueues.put(host, queue);
		}

		queue.getQueue().add(ticket);

		// Wait until ticket is cleared
		while (!ticket.isCleared()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// Interrupted
			}
		}
	}

	@Override
	public void run() {
		while (true) {

			LocalDateTime now = LocalDateTime.now();
			LocalDateTime minTime = now.minusSeconds(retentionTime);
			
			for (Entry<String, IpWaitQueue> entry : hostQueues.entrySet()) {
				IpWaitQueue waitingQueue = entry.getValue();

				if (!waitingQueue.getQueue().isEmpty()) {
					
					if(waitingQueue.getLastUse() == null || waitingQueue.getLastUse().isBefore(minTime)){
						waitingQueue.setLastUse(now);
						IpWaitTicket firstElement = waitingQueue.getQueue().pop();
						firstElement.setCleared(true);
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

}
