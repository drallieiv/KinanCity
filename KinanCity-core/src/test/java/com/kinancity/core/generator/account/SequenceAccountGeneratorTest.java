package com.kinancity.core.generator.account;

import org.junit.Test;

public class SequenceAccountGeneratorTest {

	@Test
	public void maxStartAt1Test() {
		SequenceAccountGenerator generator = getSequenceAccountGenerator();
		generator.setStartFrom(1);
		generator.setNbAccounts(99);
		generator.init();
	}

	@Test(expected = IllegalArgumentException.class)
	public void overFlowStartAt1Test() {
		SequenceAccountGenerator generator = getSequenceAccountGenerator();
		generator.setStartFrom(1);
		generator.setNbAccounts(100);
		generator.init();
	}

	@Test
	public void maxStartAt0Test() {
		SequenceAccountGenerator generator = getSequenceAccountGenerator();
		generator.setStartFrom(0);
		generator.setNbAccounts(100);
		generator.init();
	}

	@Test(expected = IllegalArgumentException.class)
	public void overFlowStartAt0Test() {
		SequenceAccountGenerator generator = getSequenceAccountGenerator();
		generator.setStartFrom(0);
		generator.setNbAccounts(101);
		generator.init();
	}

	public SequenceAccountGenerator getSequenceAccountGenerator() {
		SequenceAccountGenerator generator = new SequenceAccountGenerator();
		generator.setBaseEmail("test@mail.com");
		generator.setUsernamePattern("test**");
		return generator;
	}

}
