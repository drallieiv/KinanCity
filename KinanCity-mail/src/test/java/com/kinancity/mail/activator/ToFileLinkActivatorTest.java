package com.kinancity.mail.activator;

import org.junit.Test;

import com.kinancity.mail.Activation;

public class ToFileLinkActivatorTest {

	@Test
	public void testActivateLink() {
		ToFileLinkActivator activator = new ToFileLinkActivator();
		activator.activateLink(new Activation("Test", "test@mail.com"));
	}

}
