package com.kinancity.core.captcha.captchaai;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.CaptchaRequest;
import com.kinancity.core.captcha.captchaai.dto.response.GetTaskResulResponseDto;
import com.kinancity.core.captcha.impl.LogCaptchaCollector;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

@Slf4j
public class CaptchaaiCaptchaProviderErrorTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Properties prop;


    @Ignore
    @Test
    public void errorCodeTest() {

        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String body = "{\"errorCode\":\"ERROR_TASK_NOT_FOUND\",\"errorDescription\":\"task data has expired\",\"errorId\":1}";
        try {
            GetTaskResulResponseDto taskResponse = objectMapper.readValue(body.getBytes(), GetTaskResulResponseDto.class);
            log.debug("response : {}", taskResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}