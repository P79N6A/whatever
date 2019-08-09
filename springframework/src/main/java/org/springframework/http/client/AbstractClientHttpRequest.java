package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

    private final HttpHeaders headers = new HttpHeaders();

    private boolean executed = false;

    @Override
    public final HttpHeaders getHeaders() {
        return (this.executed ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
    }

    @Override
    public final OutputStream getBody() throws IOException {
        assertNotExecuted();
        return getBodyInternal(this.headers);
    }

    @Override
    public final ClientHttpResponse execute() throws IOException {
        assertNotExecuted();
        ClientHttpResponse result = executeInternal(this.headers);
        this.executed = true;
        return result;
    }

    protected void assertNotExecuted() {
        Assert.state(!this.executed, "ClientHttpRequest already executed");
    }

    protected abstract OutputStream getBodyInternal(HttpHeaders headers) throws IOException;

    protected abstract ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException;

}
