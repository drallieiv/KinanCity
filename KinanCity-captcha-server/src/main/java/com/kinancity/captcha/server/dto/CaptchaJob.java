package com.kinancity.captcha.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kinancity.core.captcha.antiCaptcha.dto.PtcCaptchaTask;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaptchaJob {
	Integer taskId;
	PtcCaptchaTask task;
	@JsonIgnore
	String solution;
}
