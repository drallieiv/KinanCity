package com.kinancity.mail.mailchanger.password.matcher;

import com.kinancity.mail.mailchanger.password.EmailMatcher;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * Matcher based on regular expression
 */
@Getter
public class RegexEmailMatcher implements EmailMatcher {

    private String regex;

    private Pattern exp;

    public RegexEmailMatcher(String regex) {
        this.setRegex(regex);
    }

    @Override
    public boolean matches(String accountName) {
        if(accountName == null){
            return false;
        }
        return exp.matcher(accountName).matches();
    }

    public void setRegex(String regex) {
        this.regex = regex;
        exp = Pattern.compile(regex);
    }
}
