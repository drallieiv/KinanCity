package com.kinancity.core.generator.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.kinancity.core.data.AccountData;
import com.kinancity.core.generator.AccountGenerator;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SequenceAccountGenerator implements AccountGenerator {

	private PlusTrickEmailGenerator plusTrickEmailGenerator = new PlusTrickEmailGenerator();

	private static final Pattern FORMAT_PATTERN = Pattern.compile("^[^*]*([*]+)[^*]*$");

	@Setter
	private String baseEmail;

	@Setter
	private String staticPassword;

	@Setter
	// Format with prefix and suffix. use * instead of the number sequence
	private String usernamePattern;

	@Setter
	// First value of sequence
	private int startFrom = 0;

	@Setter
	// How many accounts you want
	private int nbAccounts = 1;

	private int count = -1;

	private int sequencePadWidth;
	
	public void init(){
		
		Matcher m = FORMAT_PATTERN.matcher(usernamePattern);
		if (m.find()) {
			sequencePadWidth = m.group(1).length();
		}else{
			throw new IllegalArgumentException("pattern must contains *** where the number sequence will be");
		}
		
		if(!StringUtils.contains(baseEmail, "@")){
			throw new IllegalArgumentException("invalid or missing base email");
		}
		
		if(startFrom + nbAccounts >= Math.pow(10, sequencePadWidth)){
			throw new IllegalArgumentException("Sequence would overflow format, use more *");
		}
		
		count = startFrom;
	}

	public String generateUsername() {
		String numberStr = StringUtils.leftPad(""+count, sequencePadWidth, "0");
		return usernamePattern.replaceFirst("[*]+(?=[^*])?", numberStr);
	}

	@Override
	public AccountData nextAccountData() {
		
		if(count < 0){
			init();
		}
		
		if(count - startFrom >= nbAccounts){
			return null;
		}
		
		AccountData data = new AccountData();
		String userName = generateUsername();		
		data.setUsername(userName);
		data.setEmail(plusTrickEmailGenerator.generateEmail(baseEmail, userName));
		data.setPassword(staticPassword);

		count++;
		
		return data;
	}

}
