package com.kinancity.mail;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic cookie jar that mix all the cookies together
 * 
 * @author drallieiv
 *
 */
public class SaveAllCookieJar implements CookieJar {

	private List<Cookie> cookies = new ArrayList<>();

	@Override
	public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
		this.cookies.addAll(cookies);
	}

	@Override
	public List<Cookie> loadForRequest(HttpUrl url) {
		return cookies != null ? cookies : new ArrayList<>();
	}

	public List<Cookie> getCookies() {
		return cookies;
	}

}
