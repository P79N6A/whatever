package org.springframework.boot.web.server;

import java.security.KeyStore;

public interface SslStoreProvider {

    KeyStore getKeyStore() throws Exception;

    KeyStore getTrustStore() throws Exception;

}
