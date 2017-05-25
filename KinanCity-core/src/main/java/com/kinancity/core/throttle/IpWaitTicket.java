package com.kinancity.core.throttle;

import lombok.Data;
import lombok.Getter;

@Data
public class IpWaitTicket {

	private String host;

	private boolean cleared = false;

	public IpWaitTicket(String host) {
		this.host = host;
	}

}
