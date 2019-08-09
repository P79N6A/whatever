package org.springframework.http.client;

import org.springframework.http.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

abstract class AbstractBufferingClientHttpRequest extends AbstractClientHttpRequest {

    private ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream(1024);

    @Override
    protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
        return this.bufferedOutput;
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
        byte[] bytes = this.bufferedOutput.toByteArray();
        if (headers.getContentLength() < 0) {
            headers.setContentLength(bytes.length);
        }
        ClientHttpResponse result = executeInternal(headers, bytes);
        this.bufferedOutput = new ByteArrayOutputStream(0);
        return result;
    }

    protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException;

}
