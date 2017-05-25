package com.kinancity.mail.activator;

import org.junit.Test;

public class ToFileLinkActivatorTest {

	@Test
	public void testActivateLink() {
		ToFileLinkActivator activator = new ToFileLinkActivator();
		activator.activateLink("Test");
	}

}
