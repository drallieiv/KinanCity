package com.kinancity.core.captcha.captchaai.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@NoArgsConstructor
@Data
public class BaseRequestDto {
    private String clientKey;
    private final String appId = "CB687075-18F7-48D5-AB12-49B689674080";

    public BaseRequestDto(String clientKey) {
        this.clientKey = clientKey;
    }
}
