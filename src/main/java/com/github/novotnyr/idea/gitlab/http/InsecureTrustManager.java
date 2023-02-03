package com.github.novotnyr.idea.gitlab.http;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * A trust manager that does not validate certificate chains.
 */
public class InsecureTrustManager implements X509TrustManager {
    public static final InsecureTrustManager INSTANCE = new InsecureTrustManager();

    private InsecureTrustManager() {
        // empty constructor
    }

    public static TrustManager[] asList() {
        return new TrustManager[]{INSTANCE};
    }

    @Override
    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {
        // noop
    }

    @Override
    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {
        // noop
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
