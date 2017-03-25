package com.kinancity.api.captcha;

import com.kinancity.api.errors.tech.CaptchaSolvingException;

/**
 * Generic interface for captcha providers
 * @author drallieiv
 *
 */
public interface CaptchaProvider {

	String getCaptcha() throws CaptchaSolvingException;
}
