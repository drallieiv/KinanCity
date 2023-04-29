package com.DeathByCaptcha;

import org.json.JSONObject;


/**
 * CAPTCHA details.
 */
public class Captcha {
    public int id = 0;
    public String text = "";

    protected boolean correct = false;


    public Captcha() {
    }

    public Captcha(JSONObject src) {
        this();
        this.id = Math.max(0, src.optInt("captcha", 0));
        if (0 < this.id) {
            this.correct = src.optBoolean("is_correct", true);
            Object o = src.opt("text");
            if (JSONObject.NULL != o && null != o) {
                this.text = o.toString();
            }
        }
    }


    public boolean isUploaded() {
        return 0 < this.id;
    }

    public boolean isSolved() {
        return !this.text.equals("");
    }

    public boolean isCorrect() {
        return this.isSolved() && this.correct;
    }

    public int toInt() {
        return this.id;
    }

    public String toString() {
        return this.text;
    }

    public boolean toBoolean() {
        return this.isCorrect();
    }
}
