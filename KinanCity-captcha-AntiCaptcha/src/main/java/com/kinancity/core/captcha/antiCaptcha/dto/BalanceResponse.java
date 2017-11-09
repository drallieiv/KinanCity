package com.kinancity.core.captcha.antiCaptcha.dto;

import lombok.Data;

@Data
public class BalanceResponse {
	private Integer errorId = 0;
	private String errorCode;
	private String errorDescription;
	private Double balance;
}
