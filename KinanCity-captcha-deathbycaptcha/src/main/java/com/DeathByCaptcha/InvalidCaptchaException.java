package com.DeathByCaptcha;


public class InvalidCaptchaException extends Exception {
    public InvalidCaptchaException(String message) {
        super(message);
    }
}
