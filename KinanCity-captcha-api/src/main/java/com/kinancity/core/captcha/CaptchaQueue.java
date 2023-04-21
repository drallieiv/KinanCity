package com.kinancity.core.captcha;

import java.util.ArrayDeque;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class CaptchaQueue {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Getter
	public ArrayDeque<CaptchaRequest> queue = new ArrayDeque<>();

	private OverFlowCaptchaCollector overFlowCollector;

	public CaptchaQueue() {
		// Empty constructor
	}

	public CaptchaQueue(OverFlowCaptchaCollector overFlowCollector) {
		this.overFlowCollector = overFlowCollector;
	}

	public boolean isEmpty() {
		return queue.isEmpty();
	}

	public int size() {
		return queue.size();
	}

	/**
	 * Add request to queue
	 * 
	 * @param request
	 * @return
	 * @throws InterruptedException
	 */
	public synchronized CaptchaRequest addRequest(CaptchaRequest request) {
		if(request == null){
			throw new IllegalArgumentException("Should not add null request in queue");
		}
		queue.add(request);
		logFullQueueState();
		return request;
	}

	/**
	 * Add Multiple Captchas
	 * 
	 * @param responses
	 */
	public synchronized void addCaptchas(Collection<String> responses) {
		responses.forEach(this::addCaptcha);
	}

	/**
	 * Add a single captcha
	 * 
	 * @param response
	 */
	public synchronized void addCaptcha(String response) {
		CaptchaRequest firstInQueue = queue.poll();

		if (firstInQueue == null) {
			logger.debug("No first in queue, go to overflow");
			logger.error("In most cases this should not happen. Current Queue Info : Size {}", queue.size());
			logFullQueueState();
			overFlowCollector.manageCaptchaOverflow(response);
		} else {
			logger.debug("First in queue is {}", firstInQueue);
			firstInQueue.setResponse(response);
			logger.debug("Queue has now {} elements", queue.size());
		}
	}

	public void logFullQueueState() {
		logger.debug("Full state of the queue:");
		queue.forEach(cr -> {
			logger.debug("Request : {}", cr);
		});
	}
}
