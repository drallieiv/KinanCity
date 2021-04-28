package com.kinancity.mail;

public class EmailChangeRequest extends Activation {

    public EmailChangeRequest(String link, String email) {
        super(link, email);
    }

    public EmailChangeRequest(String link, String email, String status) {
        super(link, email, status);
    }
}
