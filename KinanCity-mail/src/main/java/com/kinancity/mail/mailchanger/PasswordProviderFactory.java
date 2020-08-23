package com.kinancity.mail.mailchanger;

import com.kinancity.mail.mailchanger.impl.StaticPasswordProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordProviderFactory {
    private static Logger LOGGER = LoggerFactory.getLogger(PasswordProviderFactory.class);

    public static PasswordProvider getPasswordProvider(String config){
        // For now only static
        LOGGER.info("Using Static Password Provider");
        return new StaticPasswordProvider(config);
    }
}
