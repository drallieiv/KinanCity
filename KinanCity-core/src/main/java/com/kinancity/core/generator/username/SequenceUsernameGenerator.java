package com.kinancity.core.generator.username;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.kinancity.core.generator.UsernameGenerator;

import lombok.Setter;

/**
 * Simple Password generator that always give the same
 * 
 * @author drallieiv
 *
 */
public class SequenceUsernameGenerator implements UsernameGenerator {

	@Setter
	// Format with prefix and suffix. use * instead of the number sequence
	private String usernamePattern;

	@Setter
	// First value of sequence
	private int startFrom = 0;

	// Remember how wide is the sequence part
	private int sequencePadWidth;

	private int current = -1;

	private boolean initDone = false;

	private static final Pattern FORMAT_PATTERN = Pattern.compile("^[^*]*([*]+)[^*]*$");

	public SequenceUsernameGenerator(String usernamePattern, int startFrom) {
		this.usernamePattern = usernamePattern;
		this.startFrom = startFrom;
		init();
	}

	public void init() {

		Matcher m = FORMAT_PATTERN.matcher(usernamePattern);
		if (m.find()) {
			sequencePadWidth = m.group(1).length();
		} else {
			throw new IllegalArgumentException("pattern must contains *** where the number sequence will be");
		}

		current = startFrom;

		initDone = true;
	}

	/**
	 * max is when all **** are 999
	 * 
	 * @return
	 */
	public double getMaxSequence() {
		return Math.pow(10, sequencePadWidth);
	}

	@Override
	public String generateUsername() {
		if (!initDone) {
			init();
		}
		String numberStr = StringUtils.leftPad("" + current, sequencePadWidth, "0");
		current++;
		return usernamePattern.replaceFirst("[*]+(?=[^*])?", numberStr);
	}

}
