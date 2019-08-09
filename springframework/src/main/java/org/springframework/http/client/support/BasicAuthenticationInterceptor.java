package org.springframework.http.client.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.Charset;

public class BasicAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private final String username;

    private final String password;

    @Nullable
    private final Charset charset;

    public BasicAuthenticationInterceptor(String username, String password) {
        this(username, password, null);
    }

    public BasicAuthenticationInterceptor(String username, String password, @Nullable Charset charset) {
        Assert.doesNotContain(username, ":", "Username must not contain a colon");
        this.username = username;
        this.password = password;
        this.charset = charset;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        if (!headers.containsKey(HttpHeaders.AUTHORIZATION)) {
            headers.setBasicAuth(this.username, this.password, this.charset);
        }
        return execution.execute(request, body);
    }

}
