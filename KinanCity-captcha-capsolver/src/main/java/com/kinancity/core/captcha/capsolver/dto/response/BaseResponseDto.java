package com.kinancity.core.captcha.capsolver.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseResponseDto {
    private Integer errorId;
    private String errorCode;
    private String errorDescription;

    @JsonIgnore
    public boolean isError() {
        return this.errorId == 1;
    }
}
