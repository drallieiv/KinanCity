package com.kinancity.core.captcha.capsolver.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTaskResponseDto extends BaseResponseDto {
    private String taskId;
}

