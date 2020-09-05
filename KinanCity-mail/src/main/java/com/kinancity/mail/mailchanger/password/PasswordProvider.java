package com.kinancity.mail.mailchanger.password;

public interface PasswordProvider {
    /**
     * Return the password for that email.
     *
     * @param email email address
     * @return the password or null if not handled
     */
    String getPassword(String email);
}
