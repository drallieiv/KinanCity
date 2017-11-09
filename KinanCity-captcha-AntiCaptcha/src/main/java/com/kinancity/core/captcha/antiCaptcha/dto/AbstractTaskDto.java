package com.kinancity.core.captcha.antiCaptcha.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({@JsonSubTypes.Type(value = PtcCaptchaTask.class, name = "PtcCaptchaTask")})
public abstract class AbstractTaskDto {
	private String type;
	private String websiteURL;
	private String websiteKey;
}
