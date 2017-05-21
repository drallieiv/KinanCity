package com.kinancity.api.errors.tech;

import com.kinancity.api.errors.TechnicalException;

/**
 * Error when IP gets softbanned on PTC site
 *  
 * @author drallieiv
 *
 */
public class IpSoftBanException extends TechnicalException {

	private static final long serialVersionUID = -1173353148853930289L;

	public IpSoftBanException(Exception e) {
		super(e);
	}

	public IpSoftBanException(String msg) {
		super(msg);
	}

}