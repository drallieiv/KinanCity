package com.kinancity.core.captcha;

import com.kinancity.core.errors.CaptchaSolvingException;

public class MockCaptchaService implements CaptchaProvider {

	@Override
	public String getCaptcha() throws CaptchaSolvingException {
		return "mockedCaptcha";
	}
}
