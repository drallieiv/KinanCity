package com.kinancity.mail.mailchanger;

public class MailChangerException extends Exception{
    public MailChangerException(String message) {
        super(message);
    }

    public MailChangerException(String message, Throwable cause) {
        super(message, cause);
    }
}
