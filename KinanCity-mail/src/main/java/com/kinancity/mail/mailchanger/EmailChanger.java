package com.kinancity.mail.mailchanger;

import com.kinancity.mail.EmailChangeRequest;

public interface EmailChanger {
    boolean acceptChange(EmailChangeRequest emailChangeRequest);
}
