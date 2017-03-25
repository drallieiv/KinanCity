package com.kinancity.api.captcha;

import com.kinancity.api.errors.tech.CaptchaSolvingException;

public class MockCaptchaService implements CaptchaProvider {

	@Override
	public String getCaptcha() throws CaptchaSolvingException {
		return "mockedCaptcha";
	}
}
