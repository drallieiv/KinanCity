package com.kinancity.mail.mailchanger;

public interface PasswordProvider {
    String getPassword(String currentEmail);
}
