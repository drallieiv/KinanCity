package com.DeathByCaptcha;

import org.json.JSONObject;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;


/**
 * Death by Captcha HTTP API client.
 */
public class HttpClient extends Client {
    final static public String CRLF = "\r\n";
    final static public String SERVER_URL = "http://api.dbcapi.me/api";


    /**
     * Proxy to use, defaults to none.
     */
    public Proxy proxy = Proxy.NO_PROXY;


    private class HttpClientCaller {
        final static public String RESPONSE_CONTENT_TYPE = "application/json";


        public String call(Proxy proxy, URL url, byte[] payload, String contentType, Date deadline)
                throws IOException, com.DeathByCaptcha.Exception {
            String response = null;
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            System.out.println("HttpClientCaller call");

            while (deadline.after(new Date()) && null != url && null == response) {
                HttpURLConnection req = null;
                try {
                    req = (HttpURLConnection) url.openConnection(proxy);
                } catch (IOException e) {
                    throw new IOException("API connection failed: " + e.toString());
                }

                req.setRequestProperty("Accept", HttpClientCaller.RESPONSE_CONTENT_TYPE);
                req.setRequestProperty("User-Agent", Client.API_VERSION);
                req.setInstanceFollowRedirects(false);

                if (0 < payload.length) {
                    try {
                        System.out.println("Method post");
                        req.setRequestMethod("POST");
                    } catch (java.lang.Exception e) {
                        //
                    }
                    req.setRequestProperty("Content-Type", contentType);
                    req.setRequestProperty("Content-Length", String.valueOf(payload.length));
                    req.setDoOutput(true);
                    OutputStream st = null;
                    try {
                        st = req.getOutputStream();
                        st.write(payload);
                        st.flush();
                    } catch (IOException e) {
                        throw new IOException("Failed sending API request: " + e.toString());
                    } finally {
                        try {
                            assert st != null;
                            st.close();
                        } catch (java.lang.Exception e) {
                            //
                        }
                    }
                    payload = new byte[0];
                } else {
                    try {
                        req.setRequestMethod("GET");
                    } catch (java.lang.Exception e) {
                        //
                    }
                }

                url = null;
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                req.setConnectTimeout(3 * intvl * 1000);
                req.setReadTimeout(3 * intvl * 1000);
                try {
                    req.connect();
                } catch (IOException e) {
                    throw new IOException("API connection failed: " + e.toString());
                }

                try {
                    int responseLength = 0;
                    int i = 1;
                    String k = null;
                    while (null != (k = req.getHeaderFieldKey(i))) {
                        if (k.equals("Content-Length")) {
                            responseLength = Integer.parseInt(req.getHeaderField(i), 10);
                        } else if (k.equals("Location")) {
                            try {
                                url = new URL(req.getHeaderField(i));
                            } catch (java.lang.Exception e) {
                                //
                            }
                        }
                        i++;
                    }

                    switch (req.getResponseCode()) {
                        case HttpURLConnection.HTTP_FORBIDDEN:
                            throw new AccessDeniedException("Access denied, check your credentials and/or balance");

                        case HttpURLConnection.HTTP_BAD_REQUEST:
                            throw new InvalidCaptchaException("CAPTCHA was rejected, check if it's a valid image");

                        case HttpURLConnection.HTTP_UNAVAILABLE:
                            throw new ServiceOverloadException("CAPTCHA was rejected due to service overload, try again later");

                        case HttpURLConnection.HTTP_SEE_OTHER:
                            if (null == url) {
                                throw new IOException("Invalid API redirection response");
                            }
                            break;
                    }

                    InputStream st = null;
                    try {
                        st = req.getInputStream();
                    } catch (IOException e) {
                        st = null;
                    } catch (java.lang.Exception e) {
                        st = req.getErrorStream();
                    }
                    if (null == st) {
                        throw new IOException("Failed receiving API response");
                    }

                    int offset = 0;
                    byte[] buff = new byte[responseLength];
                    try {
                        while (responseLength > offset) {
                            offset += st.read(buff, offset, responseLength - offset);
                        }
                        st.close();
                    } catch (IOException e) {
                        throw new IOException("Failed receiving API response: " + e.toString());
                    }
                    if (0 < buff.length) {
                        response = new String(buff, 0, buff.length);
                    }
                } catch (IOException | Exception e) {
                    throw e;
                } catch (java.lang.Exception e) {
                    throw new IOException("API communication failed: " + e.toString());
                } finally {
                    try {
                        req.disconnect();
                    } catch (java.lang.Exception e) {
                        //
                    }
                }
            }
            return response;
        }
    }


    protected JSONObject call(String cmd, byte[] data, String contentType)
            throws IOException, com.DeathByCaptcha.Exception {
        this.log("SEND", cmd);
        URL url = null;
        try {
            url = new URL(HttpClient.SERVER_URL + '/' + cmd);
        } catch (java.lang.Exception e) {
            throw new IOException("Invalid API command " + cmd);
        }
        String response = (new HttpClientCaller()).call(
                this.proxy,
                url,
                data,
                (null != contentType ? contentType : "application/x-www-form-urlencoded"),
                new Date(System.currentTimeMillis() + Client.DEFAULT_TIMEOUT * 1000)
        );
        this.log("RECV", response);
        try {
            return new JSONObject(response);
        } catch (JSONException e) {
            throw new IOException("Invalid API response");
        }
    }

    protected JSONObject call(String cmd, byte[] data)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.call(cmd, data, null);
    }

    protected JSONObject call(String cmd, JSONObject args)
            throws IOException, com.DeathByCaptcha.Exception {
        StringBuilder data = new StringBuilder();
        java.util.Iterator args_keys = args.keys();
        String k = null;
        while (args_keys.hasNext()) {
            k = args_keys.next().toString();
            try {
                data.append(URLEncoder.encode(k, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return new JSONObject();
            }
            data.append("=");
            try {
                data.append(URLEncoder.encode(args.optString(k, ""), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return new JSONObject();
            }
            if (args_keys.hasNext()) {
                data.append("&");
            }
        }
        return this.call(cmd, data.toString().getBytes());
    }

    protected JSONObject call(String cmd)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.call(cmd, new byte[0]);
    }


    /**
     * @param username username
     * @param password password
     * @see com.DeathByCaptcha.Client#Client(String, String)
     */
    public HttpClient(String username, String password) {
        super(username, password);
    }

    public HttpClient(String authtoken) {
        super(authtoken);
    }


    /**
     * @see com.DeathByCaptcha.Client#close
     */
    public void close() {
    }

    /**
     * @see com.DeathByCaptcha.Client#connect
     */
    public boolean connect()
            throws IOException {
        return true;
    }


    /**
     * @see com.DeathByCaptcha.Client#getUser
     */
    public User getUser()
            throws IOException, com.DeathByCaptcha.Exception {
        return new User(this.call("user", this.getCredentials()));
    }

    /**
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(byte[] img, String challenge, int type, byte[] banner, String banner_text, String grid)
            throws IOException, com.DeathByCaptcha.Exception {
        System.out.println("com/DeathByCaptcha/HttpClient.java#upload");
        String boundary;
        try {
            boundary = (new java.math.BigInteger(1, (java.security.MessageDigest.getInstance("SHA1")).digest(
                    (new java.util.Date()).toString().getBytes()
            ))).toString(16);
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
        String header_data = "";
        if (!this._username.equals("")) {
            header_data = "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"username\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._username.length() + HttpClient.CRLF + HttpClient.CRLF + this._username + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"password\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._password.length() + HttpClient.CRLF + HttpClient.CRLF + this._password + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"swid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + HttpClient.CRLF + Client.SOFTWARE_VENDOR_ID + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"challenge\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + challenge.length() + HttpClient.CRLF + HttpClient.CRLF + challenge + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"banner_text\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + banner_text.length() + HttpClient.CRLF + HttpClient.CRLF + banner_text + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"grid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + grid.length() + HttpClient.CRLF + HttpClient.CRLF + grid + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"type\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + Integer.toString(type).length() + HttpClient.CRLF + HttpClient.CRLF + type + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"captchafile\"; filename=\"captcha\"" + HttpClient.CRLF + "Content-Type: application/octet-stream" + HttpClient.CRLF + "Content-Length: " + img.length + HttpClient.CRLF + HttpClient.CRLF;
        } else {
            header_data = "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"authtoken\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._authtoken.length() + HttpClient.CRLF + HttpClient.CRLF + this._authtoken + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"swid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + HttpClient.CRLF + Client.SOFTWARE_VENDOR_ID + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"challenge\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + challenge.length() + HttpClient.CRLF + HttpClient.CRLF + challenge + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"banner_text\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + banner_text.length() + HttpClient.CRLF + HttpClient.CRLF + banner_text + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"grid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + grid.length() + HttpClient.CRLF + HttpClient.CRLF + grid + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"type\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + Integer.toString(type).length() + HttpClient.CRLF + HttpClient.CRLF + type + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"captchafile\"; filename=\"captcha\"" + HttpClient.CRLF + "Content-Type: application/octet-stream" + HttpClient.CRLF + "Content-Length: " + img.length + HttpClient.CRLF + HttpClient.CRLF;
        }
        byte[] hdr = header_data.getBytes();
        byte[] ftr = (HttpClient.CRLF + "--" + boundary + "--").getBytes();
        int data_length = hdr.length + img.length + ftr.length;
        byte[] banner_header_data = null;
        if (banner != null) {
            banner_header_data = ("--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"banner\"; filename=\"banner\"" + HttpClient.CRLF + "Content-Type: application/octet-stream" + HttpClient.CRLF + "Content-Length: " + banner.length + HttpClient.CRLF + HttpClient.CRLF).getBytes();
            data_length = data_length + banner_header_data.length + banner.length;
        }

        byte[] body = new byte[data_length];
        System.arraycopy(hdr, 0, body, 0, hdr.length);
        System.arraycopy(img, 0, body, hdr.length, img.length);

        //System.arraycopy(ftr, 0, body, hdr.length + img.length, ftr.length);

        if (banner != null) {
            System.arraycopy(banner_header_data, 0, body, hdr.length + img.length, banner_header_data.length);
            System.arraycopy(banner, 0, body, hdr.length + img.length + banner_header_data.length, banner.length);
            System.arraycopy(ftr, 0, body, hdr.length + img.length + banner_header_data.length + banner.length, ftr.length);
        } else {
            System.arraycopy(ftr, 0, body, hdr.length + img.length, ftr.length);
        }

        Captcha c = new Captcha(this.call("captcha", body,
                "multipart/form-data; boundary=" + boundary));
        return c.isUploaded() ? c : null;
    }

    public Captcha upload(byte[] img, String challenge, int type, byte[] banner, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.upload(img, challenge, type, banner, banner_text, "");
    }

    public Captcha upload(byte[] img, int type, byte[] banner, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception {
        return null;
    }

    public Captcha upload(byte[] img)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.upload(img, "", 0, null, "");
    }

    /**
     * @see com.DeathByCaptcha.Client#upload for noCaptchas by Token
     */
    public Captcha upload(int type, JSONObject json)
            throws IOException, com.DeathByCaptcha.Exception {
        String boundary;
        try {
            boundary = (new java.math.BigInteger(1, (java.security.MessageDigest.getInstance("SHA1")).digest(
                    (new java.util.Date()).toString().getBytes()
            ))).toString(16);
        } catch (java.security.NoSuchAlgorithmException e) {
            return null;
        }
        String extra_data_name;
        switch (type) {
            case 6:
                extra_data_name = "funcaptcha_params";
                break;
            case 7:
                extra_data_name = "hcaptcha_params";
                break;
            case 8:
                extra_data_name = "geetest_params";
                break;
            case 9:
                extra_data_name = "geetest_params";
                break;
            default:
                extra_data_name = "token_params";
                break;
        }
        String header_data = "";
        if (!this._username.equals("")) {
            header_data = "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"username\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._username.length() + HttpClient.CRLF + HttpClient.CRLF + this._username + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"password\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._password.length() + HttpClient.CRLF + HttpClient.CRLF + this._password + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"swid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + HttpClient.CRLF + Client.SOFTWARE_VENDOR_ID + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"type\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + Integer.toString(type).length() + HttpClient.CRLF + HttpClient.CRLF + type + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"" + extra_data_name + "\"" + HttpClient.CRLF + "Content-Type: application/jason" + HttpClient.CRLF + "Content-Length: " + json.toString().length() + HttpClient.CRLF + HttpClient.CRLF + json.toString() + HttpClient.CRLF;
        } else {
            header_data = "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"authtoken\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + this._authtoken.length() + HttpClient.CRLF + HttpClient.CRLF + this._authtoken + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"swid\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + HttpClient.CRLF + Client.SOFTWARE_VENDOR_ID + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"type\"" + HttpClient.CRLF + "Content-Type: text/plain" + HttpClient.CRLF + "Content-Length: " + Integer.toString(type).length() + HttpClient.CRLF + HttpClient.CRLF + type + HttpClient.CRLF +
                    "--" + boundary + HttpClient.CRLF + "Content-Disposition: form-data; name=\"" + extra_data_name + "\"" + HttpClient.CRLF + "Content-Type: application/jason" + HttpClient.CRLF + "Content-Length: " + json.toString().length() + HttpClient.CRLF + HttpClient.CRLF + json.toString() + HttpClient.CRLF;
        }
        byte[] hdr = header_data.getBytes();
        byte[] ftr = (HttpClient.CRLF + "--" + boundary + "--").getBytes();
        int data_length = hdr.length + ftr.length;


        byte[] body = new byte[data_length];
        System.arraycopy(hdr, 0, body, 0, hdr.length);

        //System.arraycopy(ftr, 0, body, hdr.length + img.length, ftr.length);


        System.arraycopy(ftr, 0, body, hdr.length, ftr.length);


        Captcha c = new Captcha(this.call("captcha", body,
                "multipart/form-data; boundary=" + boundary));
        return c.isUploaded() ? c : null;
    }

    /**
     * @see com.DeathByCaptcha.Client#getCaptcha
     */
    public Captcha getCaptcha(int id)
            throws IOException, com.DeathByCaptcha.Exception {
        return new Captcha(this.call("captcha/" + id));
    }

    /**
     * @see com.DeathByCaptcha.Client#report
     */
    public boolean report(int id)
            throws IOException, com.DeathByCaptcha.Exception {
        return !(new Captcha(this.call("captcha/" + id + "/report",
                this.getCredentials()))).isCorrect();
    }
}
