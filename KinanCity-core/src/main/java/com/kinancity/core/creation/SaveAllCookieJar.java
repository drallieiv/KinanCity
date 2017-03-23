package com.kinancity.core.creation;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Mashup all cookies not matter the URL. Use with caution
 * 
 * @author drallieiv
 *
 */
public class SaveAllCookieJar implements CookieJar {

	private List<Cookie> cookies;

	@Override
	public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
		this.cookies = cookies;
	}

	@Override
	public List<Cookie> loadForRequest(HttpUrl url) {
		return cookies != null ? cookies : new ArrayList<Cookie>();
	}

	public List<Cookie> getCookies() {
		return cookies;
	}

}
