package com.kinancity.mail.mailchanger.password.matcher;

import com.kinancity.mail.mailchanger.password.EmailMatcher;

public class AnyEmailMatcher implements EmailMatcher {

    @Override
    public boolean matches(String email) {
        return true;
    }
}
