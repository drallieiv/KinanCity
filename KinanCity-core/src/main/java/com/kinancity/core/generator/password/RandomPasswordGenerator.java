package com.kinancity.core.generator.password;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.KinanCityCli;
import com.kinancity.core.generator.PasswordGenerator;

/**
 * Random Password generator
 * @author 0815Flo0815
 *
 */
public class RandomPasswordGenerator implements PasswordGenerator {
	
	private static final char[][] CHAR_POOL = {
		{'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'},
		{'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z'},
		{'1','2','3','4','5','6','7','8','9','0'},
		{'#','?','!','@','$','%','%','^','&','>','<','+','`','*','(',')','-',']'}
	};
	private static final int MIN_LENGTH = 8;
	private static final int MAX_LENGTH = 50;

	private static Logger LOGGER = LoggerFactory.getLogger(KinanCityCli.class);
	
	private int length;

	public RandomPasswordGenerator(Integer length) {
		super();
		if (length == null) {
			LOGGER.warn("Password length must be a number! Using a random length!");
			this.length = -1;
		}
		else if (length < 0) {
			this.length = -1;
		}
		else if (length < MIN_LENGTH) {
			LOGGER.warn("The minimum password length is " + MIN_LENGTH + "! Using " + MIN_LENGTH + "!");
			this.length = MIN_LENGTH;
		}
		else if (length > MAX_LENGTH) {
			LOGGER.warn("The maximum password length is " + MAX_LENGTH + "! Using " + MAX_LENGTH + "!");
			this.length = MAX_LENGTH;
		}
		else {
			this.length = length;
		}
	}

	@Override
	public String generatePassword() {
		int length;
		if (this.length == -1) {
			length = ThreadLocalRandom.current().nextInt(MIN_LENGTH, MAX_LENGTH + 1);
		}
		else {
			length = this.length;
		}
		
		ArrayList<Character> passwordPool = new ArrayList<Character>();
		for (int i = 0; i < length; i++) {
			char[] currentPool = CHAR_POOL[i % CHAR_POOL.length];
			int randomIndexPool = ThreadLocalRandom.current().nextInt(currentPool.length);
			int randomIndexPosition = ThreadLocalRandom.current().nextInt(passwordPool.size() + 1);
			passwordPool.add(randomIndexPosition, currentPool[randomIndexPool]);
		}
		Collections.shuffle(passwordPool);
		return String.valueOf(passwordPool.stream().map(e->e.toString()).collect(Collectors.joining()));
	}
}
