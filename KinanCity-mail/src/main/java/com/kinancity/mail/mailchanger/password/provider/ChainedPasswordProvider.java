package com.kinancity.mail.mailchanger.password.provider;

import com.kinancity.mail.mailchanger.password.PasswordProvider;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class ChainedPasswordProvider implements PasswordProvider {

    private List<PasswordProvider> providerChain;

    public ChainedPasswordProvider(List<PasswordProvider> providerChain) {
        this.providerChain = providerChain;
    }

    @Override
    public String getPassword(String currentEmail) {

        for (PasswordProvider passwordProvider : providerChain) {
            String password = passwordProvider.getPassword(currentEmail);
            if(StringUtils.isNotEmpty(password)) {
                return password;
            }
        }

        return null;
    }
}
