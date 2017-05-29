package com.kinancity.core.throttle.bottleneck;

import java.util.HashMap;
import java.util.Map;

import com.kinancity.core.throttle.Bottleneck;
import com.kinancity.core.throttle.BottleneckCallback;
import com.kinancity.core.throttle.WaitTicket;
import com.kinancity.core.throttle.queue.WaitQueue;

/**
 * Bottleneck that handles a queue of requests with fixed spacing between calls
 * 
 * @author drallieiv
 *
 */
public abstract class BottleneckWithQueues<R, Q extends WaitQueue<R>> implements Bottleneck<R> {

	protected Map<R, Q> ressourceQueueMap = new HashMap<>();

	// Add a request ticket and wait for it to be cleared
	public void syncUseOf(R resource) {
		WaitTicket<R> ticket = new WaitTicket<R>(resource);
		addNewTicket(ticket);

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
	public void asyncUseOf(R resource, BottleneckCallback callback) {
		WaitTicket<R> ticket = new WaitTicket<R>(resource, callback);
		addNewTicket(ticket);
	}

	// Add in queue a new ticket for that resource
	@SuppressWarnings("unchecked")
	public synchronized void addNewTicket(WaitTicket<R> ticket) {

		R resource = ticket.getRessource();
		Q queue = ressourceQueueMap.get(resource);

		if (queue == null) {
			queue = newWaitQueue();
			ressourceQueueMap.put(resource, queue);
		}

		queue.getQueue().add(ticket);
	}

	abstract Q newWaitQueue();

}
