package com.kinancity.core.generator.impl;

import com.kinancity.core.generator.EmailGenerator;

/**
 * Generate and email using the Plus Trick
 * 
 * Output will be username@domain.com
 * 
 * @author drallieiv
 *
 */
public class CustomDomainEmailGenerator implements EmailGenerator {

	@Override
	public String generateEmail(String... args) {
		if(args.length < 2){
			throw new IllegalArgumentException("CustomDomainEmailGenerator needs a base email and the username");
		}
		
		String baseMail = args[0];
		String username = args[1];
		
		return generateEmail(baseMail, username);
	}
	
	public String generateEmail(String baseMail, String username) {	
		return baseMail.replaceAll("^[^@]*@", username+"@");
	}

}
