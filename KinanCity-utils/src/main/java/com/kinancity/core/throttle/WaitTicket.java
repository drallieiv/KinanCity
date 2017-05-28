package com.kinancity.core.throttle;

import lombok.Data;

@Data
public class WaitTicket<R> {

	private R ressource;

	private boolean cleared = false;

	private BottleneckCallback callback;

	public WaitTicket(R ressource) {
		this.ressource = ressource;
	}

	public WaitTicket(R ressource, BottleneckCallback callback) {
		this.ressource = ressource;
		this.callback = callback;
	}
	
	/**
	 * Called when the ticket is cleared to pass the bottleneck
	 */
	public void clear() {
		this.cleared = true;
		if(callback != null){
			callback.onBottleneckPass();
		}
	}

}
