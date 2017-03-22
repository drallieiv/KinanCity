package com.pallettown.core.captcha;

import com.pallettown.core.errors.CaptchaSolvingException;

/**
 * Generic interface for captcha providers
 * @author drallieiv
 *
 */
public interface CaptchaProvider {

	String getCaptcha() throws CaptchaSolvingException;
}
