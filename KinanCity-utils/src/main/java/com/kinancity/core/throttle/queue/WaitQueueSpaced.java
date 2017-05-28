package com.kinancity.core.throttle.queue;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WaitQueueSpaced<R> extends WaitQueue<R> {

	private LocalDateTime lastUse;
}
