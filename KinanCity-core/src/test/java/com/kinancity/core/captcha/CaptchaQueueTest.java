package com.kinancity.core.captcha;

import java.util.ArrayDeque;

import org.junit.Test;

public class CaptchaQueueTest {

	@Test
	public void fullTest() throws InterruptedException {
		CaptchaQueue queue = new CaptchaQueue();
		CaptchaRequest request1 = new CaptchaRequest("test1");
		CaptchaRequest request2 = new CaptchaRequest("test2");
		queue.addRequest(request1);
		queue.addRequest(request2);
		
		queue.addCaptcha("TEST");
	}

	@Test
	public void dequeueTest() {
		ArrayDeque<String> queue = new ArrayDeque<>();
		queue.add("Hello");
		queue.add("This is Dog");

		String first = queue.poll();

		System.out.println(first);
	}

	@Test
	public void requestDequeueTest() {
		ArrayDeque<CaptchaRequest> queue = new ArrayDeque<>();
		queue.add(new CaptchaRequest("Hello"));
		queue.add(new CaptchaRequest("This is Dog"));

		CaptchaRequest first = queue.poll();

		System.out.println(first.getUsername());
	}

}
