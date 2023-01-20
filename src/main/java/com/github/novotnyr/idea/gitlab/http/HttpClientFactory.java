package com.github.novotnyr.idea.gitlab.http;

import okhttp3.OkHttpClient;

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

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory)
                    .hostnameVerifier((s, sslSession) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new HttpClientException("Cannot create insecure HTTP client: " + e.getMessage(), e);
        }
    }
}
