package org.springframework.http.server.reactive;

import org.springframework.lang.Nullable;

import java.security.cert.X509Certificate;

public interface SslInfo {

    @Nullable
    String getSessionId();

    @Nullable
    X509Certificate[] getPeerCertificates();

}
