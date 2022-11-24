package com.kinancity.core.captcha.captchaai.dto.request;

import com.kinancity.core.captcha.captchaai.dto.PtcReCaptchaV2TaskProxyLess;
import lombok.Data;

@Data
public class CreateTaskRequestDto extends BaseRequestDto {

    private PtcReCaptchaV2TaskProxyLess task;

    public CreateTaskRequestDto(String clientKey) {
        super(clientKey);
        this.task = new PtcReCaptchaV2TaskProxyLess();
    }
}
