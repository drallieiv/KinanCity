package com.kinancity.mail.mailchanger;

import com.kinancity.mail.EmailChangeRequest;
import com.kinancity.mail.FileLogger;
import com.kinancity.mail.MailConstants;
import com.kinancity.mail.SaveAllCookieJar;
import com.kinancity.mail.mailchanger.password.PasswordProvider;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class DirectEmailChanger implements EmailChanger{
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final String EXPIRED_TXT = "Your attempt to change your email address is invalid or has timed out";
    private final String REQUEST_TXT = "A request has been made to change your email address";
    private final String EMAIL_UPDATED = "You have updated your email address.";

    /**
     * <input type='hidden' name='csrfmiddlewaretoken' value='FUIVBeavoU67QwR6nvOIph618Ny1fXE1apganoRYuj7VsnGJg3X9VjCRFzIUlNvb' />
     */

    private okhttp3.OkHttpClient client;

    private PasswordProvider passwordProvider;

    private SaveAllCookieJar cookieJar;

    public DirectEmailChanger(PasswordProvider passwordProvider) {
        cookieJar = new SaveAllCookieJar();
        this.client = new OkHttpClient.Builder().cookieJar(cookieJar).build();
        this.passwordProvider = passwordProvider;
    }

    @Override
    public boolean acceptChange(EmailChangeRequest emailChangeRequest) {

        try {
            Request request = new Request.Builder()
                    .header(MailConstants.HEADER_USER_AGENT, MailConstants.CHROME_USER_AGENT)
                    .url(emailChangeRequest.getLink())
                    .build();

            Response response = client.newCall(request).execute();
            String strResponse = response.body().string();
            response.body().close();

            if (response.isSuccessful()) {
                // Now check the page itself
                if (strResponse.contains(EXPIRED_TXT)) {
                    logger.info("Email Change Link Expired");
                    FileLogger.logStatus(emailChangeRequest, FileLogger.EXPIRED);
                    return false;
                }

                if (strResponse.contains(REQUEST_TXT)) {
                    logger.info("Email Change Link Valid");
                    // Parse the response
                    Document doc = Jsoup.parse(strResponse);

                    // Grab all data
                    String crsfToken = this.getCrsfToken(doc);
                    String currentEmail = this.getField(doc, "current_email");
                    String newEmail = this.getField(doc, "new_email");
                    // Add the password
                    String password = passwordProvider.getPassword(currentEmail);
                    if(password == null) {
                        logger.error("Mail Change failed : missing password for email {}", currentEmail);
                        FileLogger.logStatus(emailChangeRequest, FileLogger.ERROR);
                        return false;
                    }

                    // Send everything
                    FormBody body = new FormBody.Builder()
                            .add("secure-change-approve", "Confirm")
                            .add("csrfmiddlewaretoken", crsfToken)
                            .add("current_email", currentEmail)
                            .add("new_email", newEmail)
                            .add("current_password", password)
                            .build();

                    Request acceptRequest = new Request.Builder()
                            .header(MailConstants.HEADER_USER_AGENT, MailConstants.CHROME_USER_AGENT)
                            .header("Origin", "https://club.pokemon.com")
                            .header("referer", emailChangeRequest.getLink())
                            .url(emailChangeRequest.getLink())
                            .post(body)
                            .build();

                    Response acceptResponse = client.newCall(acceptRequest).execute();
                    cookieJar.getCookies().clear();

                    if (response.isSuccessful()) {
                        String strAcceptResponse = acceptResponse.body().string();

                        if (strAcceptResponse.contains(EMAIL_UPDATED)) {
                            logger.info("Email Change Successful");
                            FileLogger.logStatus(emailChangeRequest, FileLogger.OK);
                            return true;
                        } else {
                            logger.info("Email Change FAILED");
                            FileLogger.logStatus(emailChangeRequest, FileLogger.ERROR);
                            return false;
                        }

                    } else {
                        logger.error("Mail Change failed : Failed to call PTC to accept");
                        FileLogger.logStatus(emailChangeRequest, FileLogger.ERROR);
                        return false;
                    }
                }

            } else {
                logger.error("Mail Change failed : Failed to call PTC");
                logger.error("Mail Change Response : " + response.toString());
                FileLogger.logStatus(emailChangeRequest, FileLogger.ERROR);
                return false;
            }
        } catch (MailChangerException e) {
            logger.error("Mail Change failed : {}", e.getMessage());
            FileLogger.logStatus(emailChangeRequest, FileLogger.ERROR);
            return false;
        } catch (IOException e) {
            return false;
        }

        return false;
    }

    private String getCrsfToken(Document doc) throws MailChangerException {
        Elements tokenField = doc.select("[name=csrfmiddlewaretoken]");
        if (tokenField.isEmpty()) {
            throw new MailChangerException("CSRF Token not found");
        } else {
            return tokenField.get(0).val();
        }
    }

    private String getField(Document doc, String fieldName) throws MailChangerException {
        Elements tokenField = doc.select("[name="+fieldName+"]");
        if (tokenField.isEmpty()) {
            throw new MailChangerException(fieldName + " Field not found");
        } else {
            return tokenField.get(0).val();
        }
    }

}
