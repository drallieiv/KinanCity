package com.kinancity.core.captcha.captchaai.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PtcReCaptchaV2Task extends PtcReCaptchaV2TaskProxyLess{
    private String type = "RecaptchaV2Task";
    private String proxy;
}
