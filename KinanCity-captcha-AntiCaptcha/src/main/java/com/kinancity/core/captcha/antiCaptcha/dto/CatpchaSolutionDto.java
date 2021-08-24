package com.kinancity.core.captcha.antiCaptcha.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatpchaSolutionDto {
	@JsonProperty("gRecaptchaResponse")
	private String gRecaptchaResponse;
}
