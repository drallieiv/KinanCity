package com.kinancity.core.captcha;

/**
 * What to do if we receive a captcha but do not need it ?
 * 
 * @author drallieiv
 *
 */
public interface OverFlowCaptchaCollector {

	void manageCaptchaOverflow(String captcha);
}
