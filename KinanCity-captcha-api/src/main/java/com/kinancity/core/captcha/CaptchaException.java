package com.kinancity.core.captcha;

public class CaptchaException extends Exception {

	private static final long serialVersionUID = -8008574550607441316L;

	public CaptchaException() {
		super();
	}

	public CaptchaException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public CaptchaException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public CaptchaException(String arg0) {
		super(arg0);
	}

	public CaptchaException(Throwable arg0) {
		super(arg0);
	}

}
