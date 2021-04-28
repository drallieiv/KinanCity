package com.kinancity.core.captcha.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kinancity.core.captcha.OverFlowCaptchaCollector;

public class LogCaptchaCollector implements OverFlowCaptchaCollector {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void manageCaptchaOverflow(String captcha) {
		logger.info("We received a captcha response but it was not used : {}", captcha);
	}

}
