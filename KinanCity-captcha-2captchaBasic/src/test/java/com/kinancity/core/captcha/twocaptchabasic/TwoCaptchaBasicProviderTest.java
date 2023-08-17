package com.kinancity.core.captcha.twocaptchabasic;

import com.kinancity.api.errors.TechnicalException;
import com.kinancity.core.captcha.CaptchaException;
import com.kinancity.core.captcha.CaptchaQueue;
import com.kinancity.core.captcha.CaptchaRequest;
import com.kinancity.core.captcha.impl.LogCaptchaCollector;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class TwoCaptchaBasicProviderTest {

    @Ignore
    @Test
    public void solvingTest() throws CaptchaException, TechnicalException, InterruptedException {

        CaptchaQueue queue = new CaptchaQueue(new LogCaptchaCollector());

        String apiKey = System.getenv("apiKey") ;
        String altUrl = System.getenv("altUrl") ;

        TwoCaptchaBasicProvider provider = new TwoCaptchaBasicProvider(queue, apiKey, Optional.ofNullable(altUrl));

        log.info("Start Provider");
        new Thread(provider).start();

        log.info("Provider Started, get Balance");

        double balance = provider.getBalance();
        log.info("Balance is : {}", balance);

        CaptchaRequest request = queue.addRequest(new CaptchaRequest("test1"));
        CaptchaRequest request2 = queue.addRequest(new CaptchaRequest("test2"));

        while (request.getResponse() == null || request2.getResponse() == null) {
            Thread.sleep(500);
        }

        log.info("Response 1 given : {}", request.getResponse());
        log.info("Response 2 given : {}", request2.getResponse());

        Thread.sleep(2000);

    }
}