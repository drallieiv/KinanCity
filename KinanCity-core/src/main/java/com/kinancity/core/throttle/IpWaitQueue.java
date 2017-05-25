package com.kinancity.core.throttle;

import java.time.LocalDateTime;
import java.util.ArrayDeque;

import lombok.Data;

@Data
public class IpWaitQueue {

	private LocalDateTime lastUse;
	private ArrayDeque<IpWaitTicket> queue = new ArrayDeque<>();
	
}
