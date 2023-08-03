package com.kinancity.core.captcha.capsolver.dto.request;

import com.kinancity.core.captcha.capsolver.dto.PtcReCaptchaV2TaskProxyLess;
import lombok.Data;

@Data
public class CreateTaskRequestDto extends BaseRequestDto {

    private PtcReCaptchaV2TaskProxyLess task;

    public CreateTaskRequestDto(String clientKey) {
        super(clientKey);
        this.task = new PtcReCaptchaV2TaskProxyLess();
    }
}
