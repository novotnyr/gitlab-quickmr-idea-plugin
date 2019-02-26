package com.github.novotnyr.idea.gitlab.http;

import com.squareup.okhttp.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class HttpClientFactory {
    private static HttpClientFactory instance = new HttpClientFactory();

    private HttpClientFactory() {
        // prohibit external creation
    }

    public static HttpClientFactory getInstance() {
        return instance;
    }

    public OkHttpClient getHttpClient() {
        return new OkHttpClient();
    }

    public OkHttpClient getInsecureHttpClient() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, InsecureTrustManager.asList(), new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient client = new OkHttpClient();
            client.setSslSocketFactory(sslSocketFactory);
            client.setHostnameVerifier((s, sslSession) -> true);
            return client;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new HttpClientException("Cannot create insecure HTTP client: " + e.getMessage(), e);
        }
    }
}
