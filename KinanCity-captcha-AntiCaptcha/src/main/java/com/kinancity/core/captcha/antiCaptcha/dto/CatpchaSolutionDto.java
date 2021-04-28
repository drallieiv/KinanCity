package com.kinancity.core.captcha.antiCaptcha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CatpchaSolutionDto {
	@JsonProperty("gRecaptchaResponse")
	private String gRecaptchaResponse;
}
