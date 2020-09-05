package com.kinancity.mail.mailchanger;

import com.kinancity.mail.Activation;
import com.kinancity.mail.EmailChangeRequest;
import com.kinancity.mail.activator.LinkActivator;

public class HybridActivator implements LinkActivator {

    private LinkActivator activator;

    private EmailChanger emailChanger;

    public HybridActivator(LinkActivator activator, EmailChanger emailChanger) {
        this.activator = activator;
        this.emailChanger = emailChanger;
    }

    @Override
    public boolean activateLink(Activation link) {
        if(link instanceof EmailChangeRequest){
            return emailChanger.acceptChange((EmailChangeRequest) link);
        } else {
            return activator.activateLink(link);
        }
    }

    @Override
    public void start() {
        this.activator.start();
    }
}
