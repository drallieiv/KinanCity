package com.kinancity.core.captcha.capsolver.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BalanceResponseDto extends BaseResponseDto {
    private Double balance;
}
