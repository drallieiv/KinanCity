package com.kinancity.core.proxy;

import okhttp3.OkHttpClient;

/**
 * Proxy interface that gives a Http client through proxy
 * @author drallieiv
 *
 */
public interface HttpProxyProvider {
	OkHttpClient getClient();
}
