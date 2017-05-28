package com.kinancity.core.throttle.queue;

import java.util.ArrayDeque;

import com.kinancity.core.throttle.WaitTicket;

import lombok.Getter;
import lombok.Setter;

/**
 * Queue holding resource tickets
 *
 * @author drallieiv
 * @param <R> resource type
 */
@Getter
@Setter
public class WaitQueue<R> {

	private ArrayDeque<WaitTicket<R>> queue = new ArrayDeque<>();

}
