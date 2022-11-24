package com.kinancity.core.captcha.captchaai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class PtcReCaptchaV2TaskSolution {
    @JsonProperty("gRecaptchaResponse")
    private String gRecaptchaResponse;
}
