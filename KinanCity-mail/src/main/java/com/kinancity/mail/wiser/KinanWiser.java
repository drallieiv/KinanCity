package com.kinancity.mail.wiser;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.subethamail.wiser.Wiser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Only Accept emails to a given domain
@Slf4j
public class KinanWiser extends Wiser {

    @Setter
    private List<String> allowedDomains = new ArrayList<>();

    @Override
    public boolean accept(String from, String recipient) {
        boolean okay = super.accept(from, recipient);

        if(okay && !allowedDomains.isEmpty()) {
            okay = allowedDomains.stream().anyMatch((allowedDomain) -> recipient.endsWith(allowedDomain) );
        }

        if(!okay) {
            log.warn("Rejected email, from {} to {}", from, recipient);
        }

        return okay;
    }

    public void setAllowedDomain(String domains){
        this.allowedDomains = Arrays.asList(domains.split(","));
    }
}
