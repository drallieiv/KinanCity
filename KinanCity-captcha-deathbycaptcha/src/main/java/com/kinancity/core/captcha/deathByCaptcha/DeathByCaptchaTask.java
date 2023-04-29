package com.kinancity.core.captcha.deathByCaptcha;

import com.DeathByCaptcha.Captcha;
import com.kinancity.core.captcha.impl.BaseSolvingTask;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
public class DeathByCaptchaTask extends BaseSolvingTask {

    @Getter
    @Setter
    private Captcha captcha;

    public DeathByCaptchaTask(Captcha captcha) {
        super(String.valueOf(captcha.id));
        this.captcha = captcha;
    }
}
