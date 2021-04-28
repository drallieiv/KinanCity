package com.kinancity.mail.activator;

import java.util.ArrayList;
import java.util.List;

import com.kinancity.mail.Activation;

import lombok.Getter;
import lombok.Setter;

public class MultiThreadQueueLinkActivator extends QueueLinkActivator implements LinkActivator {

	@Setter
	@Getter
	private int nbThreads = 5;

	private List<LinkActivator> subActivators = new ArrayList<>();

	@Override
	public boolean activateLink(Activation link) {
		return super.activateLink(link);
	}

	@Override
	public void start() {
		// Start sub threads
		for (int i = 0; i < nbThreads; i++) {
			QueueLinkActivator activator = new QueueLinkActivator(this.client, this.linkQueue);
			subActivators.add(activator);
			activator.start();
		}
	}

	
}
