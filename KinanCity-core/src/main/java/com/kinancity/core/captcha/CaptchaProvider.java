package com.kinancity.core.captcha;

import com.kinancity.core.errors.CaptchaSolvingException;

/**
 * Generic interface for captcha providers
 * @author drallieiv
 *
 */
public interface CaptchaProvider {

	String getCaptcha() throws CaptchaSolvingException;
}
