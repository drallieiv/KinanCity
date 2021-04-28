package com.kinancity.mail.config;

import com.kinancity.mail.mailchanger.password.PasswordProvider;
import com.kinancity.mail.mailchanger.password.matcher.AnyEmailMatcher;
import com.kinancity.mail.mailchanger.password.matcher.MatchingPair;
import com.kinancity.mail.mailchanger.password.matcher.RegexEmailMatcher;
import com.kinancity.mail.mailchanger.password.provider.MatcherPasswordProvider;
import com.kinancity.mail.mailchanger.password.provider.StaticPasswordProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Slf4j
public class PasswordProviderFactory {
    private static Logger LOGGER = LoggerFactory.getLogger(PasswordProviderFactory.class);

    private static final String STATIC_CONFIG  = "emailChanger.password.static";
    private static final String MAPPING_CONFIG = "emailChanger.password.mapping";
    private static final String CSV_CONFIG = "emailChanger.password.csv";

    public static PasswordProvider getPasswordProvider(Properties config) {
        List<MatchingPair> mapping = new ArrayList<>();

        // CSV Mapping
        String csvConfig = config.getProperty(CSV_CONFIG);
        if(StringUtils.isNotEmpty(csvConfig)) {
            CsvPasswordReader csvReader = new CsvPasswordReader();
            mapping.addAll(csvReader.load(csvConfig));
        }

        // Regexp Mapping
        String mappingConfig = config.getProperty(MAPPING_CONFIG);
        if(StringUtils.isNotEmpty(mappingConfig)) {
            String[] configs = mappingConfig.split("\\|\\|");
            for (String sConfig : configs) {
                if(!sConfig.contains(":")){
                    log.error("Invalid mapping config [{}] => skip", sConfig);
                } else {
                    String[] data = sConfig.split(":");
                    mapping.add(new MatchingPair(new RegexEmailMatcher(data[0]), data[1]));
                }
            }

        }

        // Default static Mapping
        String staticConfig = config.getProperty(STATIC_CONFIG);
        if(StringUtils.isNotEmpty(staticConfig)) {
            mapping.add(new MatchingPair(new AnyEmailMatcher(), staticConfig));
        }

        MatcherPasswordProvider provider = new MatcherPasswordProvider();
        provider.setMatchingPairs(mapping);
        return provider;
    }
}
