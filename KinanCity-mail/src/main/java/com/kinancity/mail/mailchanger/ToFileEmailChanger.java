package com.kinancity.mail.mailchanger;

import com.kinancity.mail.EmailChangeRequest;
import com.kinancity.mail.FileLogger;

public class ToFileEmailChanger implements EmailChanger {
    @Override
    public boolean acceptChange(EmailChangeRequest emailChangeRequest) {
        FileLogger.logStatus(emailChangeRequest, FileLogger.SKIPPED);
        return true;
    }
}
