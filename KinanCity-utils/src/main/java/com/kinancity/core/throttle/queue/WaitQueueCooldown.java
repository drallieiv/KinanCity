package com.kinancity.core.throttle.queue;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WaitQueueCooldown<R> extends WaitQueue<R> {

	private int count = 0;
	private LocalDateTime maxCountReached;
	private LocalDateTime burnOutReached;

	public void countOne(){
		count++;
	}

}
