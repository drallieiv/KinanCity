package com.kinancity.mail.mailchanger.password.provider;

import com.kinancity.mail.mailchanger.password.PasswordProvider;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Fixed 1 to 1 mapping
 */
public class MappedPasswordProvider implements PasswordProvider {

    @Setter
    @Getter
    private Map<String, String> mapping;


    @Override
    public String getPassword(String currentEmail) {
        return mapping.get(currentEmail);
    }
}
