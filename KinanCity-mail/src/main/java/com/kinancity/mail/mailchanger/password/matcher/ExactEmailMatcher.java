package com.kinancity.mail.mailchanger.password.matcher;

import com.kinancity.mail.mailchanger.password.EmailMatcher;

public class ExactEmailMatcher implements EmailMatcher {

    private String email;

    public ExactEmailMatcher(String email) {
        this.email = email;
    }

    @Override
    public boolean matches(String otherEmail) {
        if(otherEmail == null || email == null){
            return false;
        }
        return otherEmail.equals(email);
    }
}
