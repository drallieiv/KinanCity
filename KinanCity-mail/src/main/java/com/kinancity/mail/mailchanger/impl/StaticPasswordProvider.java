package com.kinancity.mail.mailchanger.impl;

import com.kinancity.mail.mailchanger.PasswordProvider;

public class StaticPasswordProvider implements PasswordProvider {

    private String fixedPwd;

    public StaticPasswordProvider(String fixedPwd) {
        this.fixedPwd = fixedPwd;
    }

    @Override
    public String getPassword(String currentEmail) {
        return fixedPwd;
    }
}
