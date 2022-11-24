package com.kinancity.core.captcha.captchaai.dto.request;

import lombok.Data;

@Data
public class BalanceRequestDto extends BaseRequestDto {
    public BalanceRequestDto(String clientKey) {
        super(clientKey);
    }
}
