package com.kinancity.mail.config;

import com.kinancity.mail.mailchanger.password.PasswordProvider;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;


public class PasswordProviderFactoryTest {

    @Test
    public void getStaticPasswordProvider() {
        Properties config = new Properties();
        config.setProperty("emailChanger.password.static", "static");
        PasswordProvider factory = PasswordProviderFactory.getPasswordProvider(config);

        assertThat(factory.getPassword("anything")).isEqualTo("static");
    }

    @Test
    public void getCsvPasswordProvider() {
        Properties config = new Properties();
        config.setProperty("emailChanger.password.csv", "accounts.example.csv");
        PasswordProvider factory = PasswordProviderFactory.getPasswordProvider(config);

        assertThat(factory.getPassword("username1@myMxDomain.com")).isEqualTo("testAA123+");
        assertThat(factory.getPassword("username2@myMxDomain.com")).isEqualTo("testAA456+");
        assertThat(factory.getPassword("anything")).isNull();
    }

    @Test
    public void getRegexPasswordProvider() {
        Properties config = new Properties();
        config.setProperty("emailChanger.password.mapping", "aaaaa.*@domain.com:pass1||bbbbb[0-9]+@.*:pass2");
        PasswordProvider factory = PasswordProviderFactory.getPasswordProvider(config);

        assertThat(factory.getPassword("aaaaa123@domain.com")).isEqualTo("pass1");
        assertThat(factory.getPassword("aaaaaXYZ@domain.com")).isEqualTo("pass1");
        assertThat(factory.getPassword("bbbbb123@other.com")).isEqualTo("pass2");
        assertThat(factory.getPassword("anything")).isNull();
        assertThat(factory.getPassword("aaaaa123@other.com")).isNull();
        assertThat(factory.getPassword("bbbbbXYZ@other.com")).isNull();
    }

    @Test
    public void getFullPasswordProvider() {
        Properties config = new Properties();
        config.setProperty("emailChanger.password.csv", "accounts.example.csv");
        config.setProperty("emailChanger.password.mapping", "aaaaa.*@domain.com:pass1||bbbbb[0-9]+@.*:pass2");
        config.setProperty("emailChanger.password.static", "static");
        PasswordProvider factory = PasswordProviderFactory.getPasswordProvider(config);

        assertThat(factory.getPassword("username1@myMxDomain.com")).isEqualTo("testAA123+");
        assertThat(factory.getPassword("username2@myMxDomain.com")).isEqualTo("testAA456+");

        assertThat(factory.getPassword("aaaaa123@domain.com")).isEqualTo("pass1");
        assertThat(factory.getPassword("aaaaaXYZ@domain.com")).isEqualTo("pass1");
        assertThat(factory.getPassword("bbbbb123@other.com")).isEqualTo("pass2");

        assertThat(factory.getPassword("anything")).isEqualTo("static");
        assertThat(factory.getPassword("aaaaa123@other.com")).isEqualTo("static");
        assertThat(factory.getPassword("bbbbbXYZ@other.com")).isEqualTo("static");
    }
}