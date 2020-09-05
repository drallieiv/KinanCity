package com.kinancity.mail.mailchanger.password.provider;

import com.kinancity.mail.mailchanger.password.PasswordProvider;
import com.kinancity.mail.mailchanger.password.matcher.MatchingPair;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class MatcherPasswordProvider implements PasswordProvider {

    @Setter
    @Getter
    private List<MatchingPair> matchingPairs;


    @Override
    public String getPassword(String email) {
        for (MatchingPair pair : matchingPairs) {
            if(pair.getMatcher().matches(email)) {
                return pair.getPassword();
            }
        }
        return null;
    }
}
