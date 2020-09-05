package com.kinancity.mail.mailchanger.password.matcher;

import com.kinancity.mail.mailchanger.password.EmailMatcher;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MatchingPair {
    private EmailMatcher matcher;
    private String password;
}
