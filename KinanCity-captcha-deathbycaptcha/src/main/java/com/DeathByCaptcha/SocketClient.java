package com.DeathByCaptcha;

import org.base64.Base64;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;


/**
 * Death by Captcha socket API client.
 */
public class SocketClient extends Client {
    final static public String HOST = "api.dbcapi.me";
    final static public int FIRST_PORT = 8123;
    final static public int LAST_PORT = 8123;
    // final static public int LAST_PORT = 8130;

    final static public String TERMINATOR = "\r\n";


    protected SocketChannel channel = null;
    protected Object callLock = new Object();


    protected String sendAndReceive(byte[] payload)
            throws IOException {
        ByteBuffer sbuf = ByteBuffer.wrap(payload);
        ByteBuffer rbuf = ByteBuffer.allocateDirect(5120);
        CharsetDecoder rbufDecoder = Charset.forName("UTF-8").newDecoder();
        StringBuilder response = new StringBuilder();

        int ops = SelectionKey.OP_WRITE | SelectionKey.OP_READ;
        if (this.channel.isConnectionPending()) {
            ops = ops | SelectionKey.OP_CONNECT;
        }

        Selector selector = Selector.open();
        try {
            this.channel.register(selector, ops);
            int intvl_idx = 0;
            int intvl = 0;
            int[] results = {0, 0};

            while (true) {
                results = Client.getPollInterval(intvl_idx);
                intvl = results[0];
                intvl_idx = results[1];
                if (0 < selector.select(intvl * 1000)) {
                    Iterator keys = selector.selectedKeys().iterator();
                    while (keys.hasNext()) {
                        SelectionKey key = (SelectionKey) keys.next();
                        SocketChannel ch = (SocketChannel) key.channel();
                        if (key.isConnectable()) {
                            // Just connected
                            ch.finishConnect();
                        }
                        if (key.isReadable() && !sbuf.hasRemaining()) {
                            // Receiving the response
                            while (0 < ch.read(rbuf)) {
                                rbuf.flip();
                                response.append(rbufDecoder.decode(rbuf).toString());
                            }
                            if (2 <= response.length() && response.substring(response.length() - 2, response.length()).equals(SocketClient.TERMINATOR)) {
                                response.setLength(response.length() - 2);
                                return response.toString();
                            } else if (0 == response.length()) {
                                throw new IOException("Connection lost");
                            }
                        }
                        if (key.isWritable() && sbuf.hasRemaining()) {
                            // Sending the request
                            while (0 < ch.write(sbuf) && sbuf.hasRemaining()) {
                                //
                            }
                        }
                        keys.remove();
                    }
                }
            }
        } catch (java.lang.Exception e) {
            throw new IOException("API communication failed: " + e.toString());
        } finally {
            selector.close();
        }
    }


    /**
     * @see com.DeathByCaptcha.Client#close
     */
    public void close() {
        if (null != this.channel) {
            this.log("CLOSE");

            if (this.channel.isConnected() || this.channel.isConnectionPending()) {
                try {
                    this.channel.socket().shutdownOutput();
                    this.channel.socket().shutdownInput();
                } catch (java.lang.Exception e) {
                    //
                } finally {
                    try {
                        this.channel.close();
                    } catch (java.lang.Exception e) {
                        //
                    }
                }
            }

            try {
                this.channel.socket().close();
            } catch (java.lang.Exception e) {
                //
            }

            this.channel = null;
        }
    }

    /**
     * @see com.DeathByCaptcha.Client#connect
     */
    public boolean connect()
            throws IOException {
        if (null == this.channel) {
            this.log("OPEN");

            InetAddress host = null;
            try {
                host = InetAddress.getByName(SocketClient.HOST);
            } catch (java.lang.Exception e) {
                //System.out.println(e)
                throw new IOException("API host not found");
            }

            SocketChannel channel = SocketChannel.open();
            channel.configureBlocking(false);
            try {
                channel.connect(new InetSocketAddress(
                        host,
                        SocketClient.FIRST_PORT + new Random().nextInt(
                                SocketClient.LAST_PORT - SocketClient.FIRST_PORT + 1
                        )
                ));
            } catch (IOException e) {
                throw new IOException("API connection failed");
            }

            this.channel = channel;
        }

        return null != this.channel;
    }


    protected JSONObject call(String cmd, JSONObject args)
            throws IOException, com.DeathByCaptcha.Exception {
        try {
            args.put("cmd", cmd).put("version", Client.API_VERSION);
        } catch (JSONException e) {
            //System.out.println(e);
            return new JSONObject();
        }

        int attempts = 2;
        byte[] payload = (args.toString() + SocketClient.TERMINATOR).getBytes();
        JSONObject response = null;
        while (0 < attempts && null == response) {
            attempts--;
            if (null == this.channel && !cmd.equals("login")) {
                this.call("login", this.getCredentials());
            }
            synchronized (this.callLock) {
                if (this.connect()) {
                    this.log("SEND", args.toString());
                    try {
                        response = new JSONObject(this.sendAndReceive(payload));
                    } catch (java.lang.Exception e) {
                        //System.out.println("SocketClient.call(): " + e.toString());
                        this.close();
                    }
                }
            }
        }
        if (null == response) {
            throw new IOException("API connection lost or timed out");
        }

        this.log("RECV", response.toString());
        String error = response.optString("error", null);
        if (null != error) {
            synchronized (this.callLock) {
                this.close();
            }
            switch (error) {
                case "not-logged-in":
                    throw new AccessDeniedException("Access denied, check your credentials");
                case "banned":
                    throw new AccessDeniedException("Access denied, account is suspended");
                case "insufficient-funds":
                    throw new AccessDeniedException("Access denied, balance is too low");
                case "invalid-captcha":
                    throw new InvalidCaptchaException("CAPTCHA was rejected by the service, check if it's a valid image");
                case "service-overload":
                    throw new ServiceOverloadException("CAPTCHA was rejected due to service overload, try again later");
                default:
                    throw new IOException("API server error occured: " + error);
            }
        } else {
            return response;
        }
    }

    protected JSONObject call(String cmd)
            throws IOException, com.DeathByCaptcha.Exception {
        return this.call(cmd, new JSONObject());
    }


    /**
     * @param username username
     * @param password password
     * @see com.DeathByCaptcha.Client#Client(String, String)
     */
    public SocketClient(String username, String password) {
        super(username, password);
    }

    public SocketClient(String authtoken) {
        super(authtoken);
    }

    public void finalize() {
        this.close();
    }


    /**
     * @see com.DeathByCaptcha.Client#getUser
     */
    public User getUser()
            throws IOException, com.DeathByCaptcha.Exception {
        return new User(this.call("user"));
    }

    /**
     * @see com.DeathByCaptcha.Client#upload
     */
    public Captcha upload(byte[] img, String challenge, int type, byte[] banner, String banner_text, String grid)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject args = new JSONObject();
        try {
            args.put("captcha",
                    Base64.encodeBytes(img)).put(
                    "swid", Client.SOFTWARE_VENDOR_ID).put(
                    "challenge", challenge).put(
                    "grid", grid).put(
                    "type", Integer.toString(type)).put(
                    "banner_text", banner_text);
            if (banner != null) {
                args.put("banner",
                        Base64.encodeBytes(banner));
            }
        } catch (JSONException e) {
            //System.out.println(e);
        }
        Captcha c = new Captcha(this.call("upload", args));
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

    public Captcha upload(int type, JSONObject json)
            throws IOException, com.DeathByCaptcha.Exception {
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
        JSONObject args = new JSONObject();
        try {
            args.put("swid", Client.SOFTWARE_VENDOR_ID).put(
                    "type", Integer.toString(type)).put(
                    extra_data_name, json);

        } catch (JSONException e) {
            //System.out.println(e);
        }
        System.out.println(args);
        Captcha c = new Captcha(this.call("upload", args));
        return c.isUploaded() ? c : null;
    }


    /**
     * @see com.DeathByCaptcha.Client#getCaptcha
     */
    public Captcha getCaptcha(int id)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject args = new JSONObject();
        try {
            args.put("captcha", id);
        } catch (JSONException e) {
            //System.out.println(e);
        }
        return new Captcha(this.call("captcha", args));
    }

    /**
     * @see com.DeathByCaptcha.Client#report
     */
    public boolean report(int id)
            throws IOException, com.DeathByCaptcha.Exception {
        JSONObject args = new JSONObject();
        try {
            args.put("captcha", id);
        } catch (JSONException e) {
            //System.out.println(e);
        }
        return !(new Captcha(this.call("report", args))).isCorrect();
    }
}
