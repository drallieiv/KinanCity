package com.DeathByCaptcha;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;


/**
 * Base Death by Captcha API client.
 */
abstract public class Client {
    final static public String API_VERSION = "DBC/Java v4.6";
    final static public int SOFTWARE_VENDOR_ID = 0;

    final static public int DEFAULT_TIMEOUT = 60;
    final static public int DEFAULT_TOKEN_TIMEOUT = 120;
    final static public int[] POLLS_INTERVAL = {1, 1, 2, 3, 2, 2, 3, 2, 2};
    final static public int DFLT_POLL_INTERVAL = 3;
    final static public int LEN_POLLS_INTERVAL = POLLS_INTERVAL.length;


    /**
     * Client verbosity flag.
     * <p>
     * When it's set to true, the client will dump API calls for debug purpose.
     */
    public boolean isVerbose = false;


    protected String _username = "";
    protected String _password = "";
    protected String _authtoken = "";


    protected void log(String call, String msg) {
        if (this.isVerbose) {
            System.out.println((System.currentTimeMillis() / 1000) + " " +
                    call + (null != msg ? ": " + msg : ""));
        }
    }

    protected void log(String call) {
        this.log(call, null);
    }

    protected JSONObject getCredentials() {
        try {
            if (_username.equals("")) {
                System.out.println("Using authtoken");
                return new JSONObject().put("authtoken", this._authtoken);
            } else {
                return new JSONObject().put("username", this._username).put("password", this._password);
            }
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    protected byte[] load(InputStream st)
            throws IOException {
        int n = 0, offset = 0;
        byte[] img = new byte[0];
        while (true) {
            try {
                n = st.available();
            } catch (IOException e) {
                n = 0;
            }
            if (0 < n) {
                if (offset + n > img.length) {
                    img = java.util.Arrays.copyOf(img, img.length + n);
                }
                offset += st.read(img, offset, n);
            } else {
                break;
            }
        }
        return img;
    }

    protected byte[] load(File f)
            throws IOException, FileNotFoundException {
        InputStream st = new FileInputStream(f);
        try {
            return this.load(st);
        } catch (FileNotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            st.close();
        }
    }

    public byte[] load(String fn)
            throws IOException, FileNotFoundException {
        return this.load(new File(fn));
    }


    /**
     * Closes opened connections (if any), cleans up resources.
     */
    abstract public void close();

    /**
     * Opens API-specific connection if not opened yet.
     *
     * @return true on success
     * @throws IOException IO exception
     */
    abstract public boolean connect()
            throws IOException;


    /**
     * @param username DBC account username
     * @param password DBC account password
     */
    public Client(String username, String password) {
        this._username = username;
        this._password = password;
        this._authtoken = "";
    }

    /**
     * @param authtoken DBC account authtoken
     */
    public Client(String authtoken) {
        this._username = "";
        this._password = "";
        this._authtoken = authtoken;
    }


    /**
     * Fetches user details.
     *
     * @return user details object
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     */
    abstract public User getUser()
            throws IOException, com.DeathByCaptcha.Exception;

    /**
     * Fetches user balance (in US cents).
     *
     * @return user balance
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     */
    public double getBalance()
            throws IOException, com.DeathByCaptcha.Exception {
        return this.getUser().balance;
    }


    /**
     * Uploads a CAPTCHA to the service.
     *
     * @param img         CAPTCHA image byte vector
     * @param grid        Specifies what grid individual images in captcha are aligned to, Ex.: "2x4"(width x height).
     *                    If not supplied, dbc will attempt to autodetect the grid.
     * @param challenge   challenge
     * @param type        type
     * @param banner      banner
     * @param banner_text banner_text
     * @return CAPTCHA object on success, null otherwise
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     */
    abstract public Captcha upload(byte[] img, String challenge, int type, byte[] banner, String banner_text, String grid)
            throws IOException, com.DeathByCaptcha.Exception;

    abstract public Captcha upload(byte[] img, String challenge, int type, byte[] banner, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception;

    abstract public Captcha upload(byte[] img, int type, byte[] banner, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception;

    abstract public Captcha upload(byte[] img)
            throws IOException, com.DeathByCaptcha.Exception;

    /**
     * @param st CAPTCHA image stream
     * @return this.upload
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(InputStream st)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.upload(this.load(st));
    }

    /**
     * @param f CAPTCHA image file
     * @return this.upload
     * @throws IOException                  IO exception
     * @throws FileNotFoundException        File not found
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(File f)
            throws IOException, FileNotFoundException, com.DeathByCaptcha.Exception {
        return this.upload(this.load(f));
    }

    /**
     * @param fn CAPTCHA image file name
     * @return this.upload
     * @throws IOException                  IO exception
     * @throws FileNotFoundException        File not found
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(String fn)
            throws IOException, FileNotFoundException, com.DeathByCaptcha.Exception {
        return this.upload(this.load(fn));
    }

    /**
     * A method created to upload reCAPTCHAs v2, Funcaptchas and Hcaptchas with proxy
     *
     * @param type    Captcha type
     * @param key     googlekey, sitekey or publickey
     * @param pageurl Site url
     * @return this.upload
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(int type, String key, String pageurl)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject json = new JSONObject();
        try {
            switch (type) {
                case 6:
                    json.put("publickey", key);
                    break;
                case 7:
                    json.put("sitekey", key);
                    break;
                default:
                    json.put("googlekey", key);
                    break;
            }

            json.put("pageurl", pageurl);

        } catch (JSONException e) {
            //System.out.println(e);
        }
        return this.upload(type, json);
    }

    /**
     * A method created to upload reCAPTCHAs v2, Funcaptchas and Hcaptchas with proxy
     *
     * @param type      Captcha type
     * @param proxy     User proxy
     * @param proxytype User proxy type
     * @param key       googlekey, sitekey or publickey
     * @param pageurl   Site url
     * @return this.upload
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(int type, String proxy, String proxytype, String key, String pageurl)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("proxy", proxy);
            json.put("proxytype", proxytype);

            switch (type) {
                case 6:
                    json.put("publickey", key);
                    break;
                case 7:
                    json.put("sitekey", key);
                    break;
                default:
                    json.put("googlekey", key);
                    break;
            }

            json.put("pageurl", pageurl);

        } catch (JSONException e) {
            //System.out.println(e);
        }
        System.out.println(json);
        return this.upload(type, json);
    }

    /**
     * A method created to upload reCAPTCHAs v2 Google search captchas
     *
     * @param type      Captcha type
     * @param googlekey Site Google Key
     * @param pageurl   Site url
     * @param data_s    Only required for solve the google search tokens
     * @throws IOException
     * @throws com.DeathByCaptcha.Exception
     */
    public Captcha upload(int type, String googlekey, String pageurl, String data_s)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("googlekey", googlekey);
            json.put("pageurl", pageurl);
            json.put("data-s", data_s);
        } catch (JSONException e) {
            //System.out.println(e);
        }
        return this.upload(type, json);
    }

    /**
     * A method created to upload reCAPTCHAs v3.
     *
     * @param type      Captcha type
     * @param googlekey Site Google Key
     * @param pageurl   Site url
     * @param action    Action that trigger reCAPTCHA v3 validation
     * @param min_score Minimum score acceptable from recaptchaV3
     * @return this.upload
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(int type, String googlekey, String pageurl, String action, double min_score)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("googlekey", googlekey);
            json.put("pageurl", pageurl);
            json.put("action", action);
            json.put("min_score", min_score);
        } catch (JSONException e) {
            //System.out.println(e);
        }
        return this.upload(type, json);
    }

    /**
     * A method created to upload reCAPTCHAs v3 with proxy
     *
     * @param type      Captcha type
     * @param proxy     User proxy
     * @param proxytype User proxy type
     * @param googlekey Site Google Key
     * @param pageurl   Site url
     * @param action    Action that trigger reCAPTCHA v3 validation
     * @param min_score Minimum score acceptable from recaptchaV3
     * @return this.upload
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(int type, String proxy, String proxytype, String googlekey, String pageurl, String action, double min_score)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject json = new JSONObject();
        try {
            json.put("proxy", proxy);
            json.put("proxytype", proxytype);
            json.put("googlekey", googlekey);
            json.put("pageurl", pageurl);
            json.put("action", action);
            json.put("min_score", min_score);
        } catch (JSONException e) {
            //System.out.println(e);
        }
        return this.upload(type, json);
    }

    abstract public Captcha upload(int type, JSONObject json)
            throws IOException, com.DeathByCaptcha.Exception;


    /**
     * Fetches an uploaded CAPTCHA details.
     *
     * @param id CAPTCHA ID
     * @return CAPTCHA object if found, null otherwise
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     */
    abstract public Captcha getCaptcha(int id)
            throws IOException, com.DeathByCaptcha.Exception;

    /**
     * @param captcha CAPTCHA object
     * @return CAPTCHA
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#getCaptcha
     */
    public Captcha getCaptcha(Captcha captcha)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.getCaptcha(captcha.id);
    }


    /**
     * Fetches an uploaded CAPTCHA text.
     *
     * @param id CAPTCHA ID
     * @return CAPTCHA text if solved, null otherwise
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     */
    public String getText(int id)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.getCaptcha(id).text;
    }

    /**
     * @param captcha CAPTCHA object
     * @return this.getText text if solved, null otherwise
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#getText
     */
    public String getText(Captcha captcha)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.getText(captcha.id);
    }


    /**
     * Reports an incorrectly solved CAPTCHA
     *
     * @param id CAPTCHA ID
     * @return true on success
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     */
    abstract public boolean report(int id)
            throws IOException, com.DeathByCaptcha.Exception;

    /**
     * @param captcha CAPTCHA object
     * @return this.report captcha report
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#report
     */
    public boolean report(Captcha captcha)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.report(captcha.id);
    }


    /**
     * Tries to solve a CAPTCHA by uploading it and polling for its status
     * and text with arbitrary timeout.
     *
     * @param img         CAPTCHA image byte vector
     * @param challenge   challenge
     * @param type        type
     * @param banner      banner
     * @param banner_text banner_text
     * @param grid        Specifies what grid individual images in captcha are aligned to, Ex.: "2x4"(width x height).
     *                    If not supplied, dbc will attempt to autodetect the grid.
     * @param timeout     Solving timeout (in seconds)
     * @return CAPTCHA object if uploaded and correctly solved, null otherwise
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws InterruptedException         interruption error
     */
    public Captcha decode(byte[] img, String challenge, int type, byte[] banner, String banner_text, String grid, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TIMEOUT) * 1000;
        Captcha captcha = this.upload(img, challenge, type, banner, banner_text, grid);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    public Captcha decode(byte[] img, String challenge, int type, byte[] banner, String banner_text, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(img, challenge, type, banner, banner_text, "", 0);
    }


    public Captcha decode(byte[] img, int type, byte[] banner, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(img, "", type, banner, banner_text, 0);
    }

    public Captcha decode(byte[] img, int type)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(img, "", type, null, "", 0);
    }

    public Captcha decode(byte[] img, String challenge)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(img, challenge, 0, null, "", 0);
    }

    public Captcha decode(byte[] img, int type, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(img, "", type, null, "", timeout);
    }

    public Captcha decode(byte[] img, String challenge, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(img, challenge, 0, null, "", timeout);
    }

    /**
     * @param img CAPTCHA image
     * @return this.decode decoded captcha
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws InterruptedException         interruption error
     * @see com.DeathByCaptcha.Client#decode(byte[], int)
     */
    public Captcha decode(byte[] img)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(img, "", 0, null, "", 0);
    }

    /**
     * @param st          CAPTCHA image stream
     * @param challenge   challenge
     * @param type        type
     * @param banner_st   banner_st
     * @param banner_text banner_text
     * @param grid        Specifies what grid individual images in captcha are aligned to, Ex.: "2x4"(width x height).
     *                    If not supplied, dbc will attempt to autodetect the grid.
     * @param timeout     Solving timeout (in seconds)
     * @return this.decode decoded captcha
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws InterruptedException         interruption error
     * @see com.DeathByCaptcha.Client#decode
     */
    public Captcha decode(InputStream st, String challenge, int type, InputStream banner_st, String banner_text, String grid, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(st), challenge, type, this.load(banner_st), banner_text, grid, timeout);
    }

    public Captcha decode(InputStream st, String challenge, int type, InputStream banner_st, String banner_text, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(st), challenge, type, this.load(banner_st), banner_text, "", timeout);
    }

    public Captcha decode(InputStream st, int type, InputStream banner_st, String banner_text, String grid)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(st, "", type, banner_st, banner_text, grid, 0);
    }

    public Captcha decode(InputStream st, int type, InputStream banner_st, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(st, "", type, banner_st, banner_text, "", 0);
    }

    public Captcha decode(InputStream st, int type, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(st), "", type, null, "", timeout);
    }

    public Captcha decode(InputStream st, String challenge)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(st), challenge, 0, null, "", 0);
    }

    public Captcha decode(InputStream st, String challenge, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(st), challenge, 0, null, "", timeout);
    }

    public Captcha decode(InputStream st, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(st), timeout);
    }

    /**
     * @param st stream
     * @return this.decode decoded captcha
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws InterruptedException         interruption error
     * @see com.DeathByCaptcha.Client#decode(InputStream, int)
     */
    public Captcha decode(InputStream st)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(st, 0);
    }

    /**
     * @param f           CAPTCHA image file
     * @param challenge   challenge
     * @param type        type
     * @param banner_f    banner_f
     * @param banner_text banner_text
     * @param timeout     Solving timeout (in seconds)
     * @return this.decode decoded captcha
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws InterruptedException         interruption error
     * @see com.DeathByCaptcha.Client#decode
     */

    public Captcha decode(File f, String challenge, int type, File banner_f, String banner_text, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(f), challenge, type, this.load(banner_f), banner_text, timeout);
    }

    public Captcha decode(File f, int type, File banner_f, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(f, "", type, banner_f, banner_text, 0);
    }

    public Captcha decode(File f, int type, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(f), "", type, null, "", timeout);
    }

    public Captcha decode(File f, String challenge)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(f), challenge, 0, null, "", 0);
    }

    public Captcha decode(File f, String challenge, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(f), challenge, 0, null, "", timeout);
    }

    public Captcha decode(File f, int timeout)
            throws IOException, FileNotFoundException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(f), timeout);
    }

    /**
     * @param f CAPTCHA file
     * @return this.decode decoded CAPTCHA
     * @throws IOException                  IO error
     * @throws FileNotFoundException        File not found error
     * @throws com.DeathByCaptcha.Exception Own Exception
     * @throws InterruptedException         Interrupted error
     * @see com.DeathByCaptcha.Client#decode(File, int)
     */
    public Captcha decode(File f)
            throws IOException, FileNotFoundException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(f, 0);
    }

    /**
     * @param fn          CAPTCHA image file name
     * @param challenge   challenge
     * @param type        type
     * @param banner_fn   banner_fn
     * @param banner_text banner_text
     * @param timeout     Solving timeout (in seconds)
     * @return this.decode decoded captcha
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws InterruptedException         interruption error
     * @see com.DeathByCaptcha.Client#decode
     */

    public Captcha decode(String fn, String challenge, int type, String banner_fn, String banner_text, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(fn), challenge, type, this.load(banner_fn), banner_text, timeout);
    }

    /**
     * @param fn          CAPTCHA image file name
     * @param challenge   challenge
     * @param type        type
     * @param banner_fn   banner_fn
     * @param banner_text banner_text
     * @param grid        Specifies what grid individual images in captcha are aligned to, Ex.: "2x4"(width x height).
     *                    If not supplied, dbc will attempt to autodetect the grid.
     * @param timeout     Solving timeout (in seconds)
     * @return this.decode decoded captcha
     * @throws IOException                  IO exception
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws InterruptedException         interruption error
     * @see com.DeathByCaptcha.Client#decode
     */

    public Captcha decode(String fn, String challenge, int type, String banner_fn, String banner_text, String grid, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(fn), challenge, type, this.load(banner_fn), banner_text, grid, timeout);
    }

    public Captcha decode(String fn, int type, String banner_fn, String banner_text)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(fn, "", type, banner_fn, banner_text, 0);
    }

    /**
     * A method created to decode new reCAPTCHAs Image Group type.
     * See the <a href="https://deathbycaptcha.com/api/newrecaptcha">Api Documentation<a/> for more information.
     *
     * @param fn          The relative or absolute path of the image file or the url of the image.
     * @param type        Type of captcha (3)
     * @param banner_fn   The relative or absolute path of the banner image or the url of the banner image
     * @param banner_text The banner text
     * @param timeout     Solving timeout (in seconds)
     * @throws IOException
     * @throws com.DeathByCaptcha.Exception
     * @throws InterruptedException
     * @see com.DeathByCaptcha.Client#decode(String, int, String, String, String, int)
     */
    public Captcha decode(String fn, int type, String banner_fn, String banner_text, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(fn, "", type, banner_fn, banner_text, timeout);
    }

    /**
     * A method created to decode new reCAPTCHAs Image Group type.
     * See the <a href="https://deathbycaptcha.com/api/newrecaptcha">Api Documentation<a/> for more information.
     *
     * @param fn          The relative or absolute path of the image file or the url of the image.
     * @param type        Type of captcha (3)
     * @param banner_fn   The relative or absolute path of the banner image or the url of the banner image
     * @param banner_text The banner text
     * @param grid        Specifies what grid individual images in captcha are aligned to, Ex.: "2x4"(width x height).
     *                    If not supplied, dbc will attempt to autodetect the grid.
     * @param timeout     Solving timeout (in seconds)
     * @throws IOException
     * @throws com.DeathByCaptcha.Exception
     * @throws InterruptedException
     * @see com.DeathByCaptcha.Client#decode(String, int, String, String, int)
     */
    public Captcha decode(String fn, int type, String banner_fn, String banner_text, String grid, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(fn, "", type, banner_fn, banner_text, grid, timeout);
    }

    public Captcha decode(String fn, int type, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(fn), "", type, null, "", timeout);
    }

    public Captcha decode(String fn, String challenge)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(fn), challenge, 0, null, "", 0);
    }

    public Captcha decode(String fn, String challenge, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(fn), challenge, 0, null, "", timeout);
    }

    public Captcha decode(String fn, int timeout)
            throws IOException, FileNotFoundException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(this.load(fn), timeout);
    }

    /**
     * @param fn CAPTCHA image file
     * @return this.decode decoded string
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @throws FileNotFoundException        File exception
     * @see com.DeathByCaptcha.Client#decode(String, int)
     */
    public Captcha decode(String fn)
            throws IOException, FileNotFoundException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(fn, 0);
    }

    /**
     * A method created to decode reCAPTCHAs v2, funcaptchas and hcaptchas.
     * See for more info:
     * <ul>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">reCAPTCHA v2 Documentation</a>,
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/funcaptcha">Funcaptcha Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/hcaptcha">Hcaptcha Documentation</a>
     *     </li>
     * </ul>
     *
     * @param type    Type of captcha (4 for reCAPTCHA v2, 6 for Funcaptcha or 7 for Hcaptcha).
     * @param key     googlekey, publickey or sitekey depending of type of captcha.
     * @param pageurl The url of the page with the reCAPTCHA v2, Funcaptcha or Hcaptcha challenge.
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(int, String, String, int)
     */
    public Captcha decode(int type, String key, String pageurl)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(type, key, pageurl, 0);
    }

    /**
     * A method created to decode reCAPTCHAs v2, funcaptchas and hcaptchas.
     * See for more info:
     * <ul>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">reCAPTCHA v2 Documentation</a>,
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/funcaptcha">Funcaptcha Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/hcaptcha">Hcaptcha Documentation</a>
     *     </li>
     * </ul>
     *
     * @param type    Type of captcha (4 for reCAPTCHA v2, 6 for Funcaptcha or 7 for Hcaptcha).
     * @param key     googlekey, publickey or sitekey depending of type of captcha.
     * @param pageurl The url of the page with the reCAPTCHA v2, Funcaptcha or Hcaptcha challenge.
     * @param timeout Solving timeout (in seconds).
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(int, String, String, int)
     */
    public Captcha decode(int type, String key, String pageurl, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(type, key, pageurl);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    /**
     * A method created to decode reCAPTCHAs v2 with proxy.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">API Documentation</a>
     * for more info.
     *
     * @param proxy     Proxy url and credentials (if any)
     * @param proxytype Your proxy connection protocol (http)
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the captcha challenges
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(String, String, String, String, String, double, int)
     * @see com.DeathByCaptcha.Client#decode(int, String, String, String, String)
     */
    public Captcha decode(String proxy, String proxytype, String googlekey, String pageurl)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(proxy, proxytype, googlekey, pageurl, 0);
    }

    /**
     * A method created to decode reCAPTCHAs v2 with proxy.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">API Documentation</a>
     * for more info.
     *
     * @param proxy     Proxy url and credentials (if any).
     * @param proxytype Your proxy connection protocol (http).
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the recaptcha challenges.
     * @param timeout   Solving timeout (in seconds).
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(int, String, String, String, String)
     */
    public Captcha decode(String proxy, String proxytype, String googlekey, String pageurl, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(4, proxy, proxytype, googlekey, pageurl);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    /**
     * A method created to decode reCAPTCHAs v2 Google search captchas.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">API Documentation</a>
     * for more info.
     *
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the recaptcha challenges
     * @param data_s    Only required for solve the google search tokens, while google search trigger the robot protection.
     *                  Use the data-s value inside the google search response html. For regulars tokens don't use this parameter.
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(String, String, String, String, String, double, int)
     * @see com.DeathByCaptcha.Client#decode(int, String, String, String, String)
     */
    public Captcha decode(String googlekey, String pageurl, String data_s)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(googlekey, pageurl, data_s, 0);
    }

    /**
     * A method created to decode reCAPTCHAs v2 Google search captchas.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">API Documentation</a>
     * for more info.
     *
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the recaptcha challenges.
     * @param timeout   Solving timeout (in seconds).
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(int, String, String, String, String)
     */
    public Captcha decode(String googlekey, String pageurl, String data_s, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(4, googlekey, pageurl, data_s);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    /**
     * A method created to decode reCAPTCHAs v3.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#reCAPTCHAv3">API Documentation</a>
     * for more info.
     *
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the recaptcha challenges.
     * @param action    The action name.
     * @param min_score The minimal score, usually 0.3.
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(String, String, String, String, String, double, int)
     */
    public Captcha decode(String googlekey, String pageurl, String action, double min_score)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(googlekey, pageurl, action, min_score, 0);
    }


    /**
     * A method created to decode reCAPTCHAs v3.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#reCAPTCHAv3">API Documentation</a>
     * for more info.
     *
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the recaptcha challenges.
     * @param action    The action name.
     * @param min_score The minimal score, usually 0.3.
     * @param timeout   Solving timeout (in seconds).
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload(int, String, String, String, String, String, double)
     */
    public Captcha decode(String googlekey, String pageurl, String action, double min_score, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(5, googlekey, pageurl, action, min_score);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    /**
     * A method created to decode reCAPTCHAs v3 with proxy.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#reCAPTCHAv3">API Documentation</a>
     * for more info.
     *
     * @param proxy     Proxy url and credentials (if any).
     * @param proxytype Your proxy connection protocol (http).
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the recaptcha challenges.
     * @param action    The action name.
     * @param min_score The minimal score, usually 0.3.
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(String, String, String, String, String, double, int)
     */
    public Captcha decode(String proxy, String proxytype, String googlekey, String pageurl, String action, double min_score)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(proxy, proxytype, googlekey, pageurl, action, min_score, 0);
    }


    /**
     * A method created to decode reCAPTCHAs v3 with proxy.
     * See <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#reCAPTCHAv3">API Documentation</a>
     * for more info.
     *
     * @param proxy     Proxy url and credentials (if any).
     * @param proxytype Your proxy connection protocol (http).
     * @param googlekey The google recaptcha site key of the website with the recaptcha.
     * @param pageurl   The url of the page with the recaptcha challenges.
     * @param action    The action name.
     * @param min_score The minimal score, usually 0.3.
     * @param timeout   Solving timeout (in seconds).
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload(int, String, String, String, String, String, double)
     */
    public Captcha decode(String proxy, String proxytype, String googlekey, String pageurl, String action, double min_score, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(5, proxy, proxytype, googlekey, pageurl, action, min_score);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    /**
     * A method created to decode reCAPTCHAs v2, funcaptchas and hcaptchas with proxy.
     * See for more info:
     * <ul>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">reCAPTCHA v2 Documentation</a>,
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/funcaptcha">Funcaptcha Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/hcaptcha">Hcaptcha Documentation</a>
     *     </li>
     * </ul>
     *
     * @param type      Type of captcha (4 for reCAPTCHA v2, 6 for Funcaptcha or 7 for Hcaptcha).
     * @param proxy     Proxy url and credentials (if any).
     * @param proxytype Your proxy connection protocol (http).
     * @param key       googlekey, publickey or sitekey depending of type of captcha.
     * @param pageurl   The url of the page with the reCAPTCHA v2, Funcaptcha or Hcaptcha challenge.
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(int, String, String, String, String, int)
     * @see com.DeathByCaptcha.Client#upload(int, String, String, String, String)
     */
    public Captcha decode(int type, String proxy, String proxytype, String key, String pageurl)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(type, proxy, proxytype, key, pageurl, 0);
    }

    /**
     * A method created to decode reCAPTCHA v2, funcaptchas and hcaptchas with proxy.
     * See for more info:
     * <ul>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">reCAPTCHA v2 Documentation</a>,
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/funcaptcha">Funcaptcha Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/hcaptcha">Hcaptcha Documentation</a>
     *     </li>
     * </ul>
     *
     * @param type      Type of captcha (4 for reCAPTCHA v2, 6 for Funcaptcha or 7 for Hcaptcha).
     * @param proxy     Proxy url and credentials (if any).
     * @param proxytype Your proxy connection protocol (http).
     * @param key       googlekey, publickey or sitekey depending of type of captcha.
     * @param pageurl   The url of the page with the reCAPTCHA v2, Funcaptcha or Hcaptcha challenge.
     * @param timeout   Solving timeout (in seconds).
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#upload(int, String, String, String, String)
     */
    public Captcha decode(int type, String proxy, String proxytype, String key, String pageurl, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(type, proxy, proxytype, key, pageurl);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    public Captcha decode(JSONObject json)
            throws IOException, FileNotFoundException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(json, 0);
    }

    public Captcha decode(JSONObject json, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(4, json);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    /**
     * A method created to decode reCAPTCHAs v2, reCAPTCHAs v3, funcaptchas and hcaptchas params in JSON format.
     * See for more info:
     * <ul>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">reCAPTCHA v2 Documentation</a>,
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#reCAPTCHAv3">reCAPTCHA v3 Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/funcaptcha">Funcaptcha Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/hcaptcha">Hcaptcha Documentation</a>
     *     </li>
     * </ul>
     *
     * @param type Type of captcha (4 for reCAPTCHA v2, 5 for reCAPTCHA v3, 6 for Funcaptcha and 7 for Hcaptcha).
     * @param json The <b>token_params</b> (reCAPTCHA v2 and reCAPTCHA v3), <b>funcaptcha_params</b> or
     *             <b>hcaptcha_params</b>.
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(int, JSONObject, int)
     */
    public Captcha decode(int type, JSONObject json)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        return this.decode(type, json, 0);
    }

    /**
     * A method created to decode reCAPTCHA v2, reCAPTCHA v3, funcaptchas and hcaptchas params in JSON format.
     * See for more info:
     * <ul>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#token-doc">reCAPTCHA v2 Documentation</a>,
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/newtokenrecaptcha#reCAPTCHAv3">reCAPTCHA v3 Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/funcaptcha">Funcaptcha Documentation</a>
     *     </li>
     *     <li>
     *         <a href="https://deathbycaptcha.com/api/hcaptcha">Hcaptcha Documentation</a>
     *     </li>
     * </ul>
     *
     * @param type    Type of captcha (4 for reCAPTCHA v2, 5 for reCAPTCHA v3, 6 for Funcaptcha and 7 for Hcaptcha).
     * @param json    The <b>token_params</b> (reCAPTCHA v2 and reCAPTCHA v3), <b>funcaptcha_params</b> or
     *                <b>hcaptcha_params</b>.
     * @param timeout Solving timeout (in seconds).
     * @throws IOException                  IO error
     * @throws InterruptedException         Interruption error
     * @throws com.DeathByCaptcha.Exception Own exception
     * @see com.DeathByCaptcha.Client#decode(int, JSONObject, int)
     */
    public Captcha decode(int type, JSONObject json, int timeout)
            throws IOException, com.DeathByCaptcha.Exception, InterruptedException {
        long deadline = System.currentTimeMillis() + (0 < timeout ? timeout : Client.DEFAULT_TOKEN_TIMEOUT) * 1000;
        Captcha captcha = this.upload(type, json);

        if (null != captcha) {
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (deadline > System.currentTimeMillis() && !captcha.isSolved()) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                Thread.sleep(intvl * 1000);
                captcha = this.getCaptcha(captcha.id);
            }
            if (captcha.isSolved() && captcha.isCorrect()) {
                return captcha;
            }
        }
        return null;
    }

    /**
     * @param idx index of POLLS_INTERVAL
     * @return position of POLLS_INTERVAL or default
     * polling time if index out of range
     */
    public static int[] getPollInterval(int idx) {
        int intvl = 0;

        if (Client.LEN_POLLS_INTERVAL > idx) {
            intvl = Client.POLLS_INTERVAL[idx];
        } else {
            intvl = Client.DFLT_POLL_INTERVAL;
        }

        return new int[]{intvl, ++idx};
    }
}
