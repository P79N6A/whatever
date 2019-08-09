package org.springframework.http.client;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

final class HttpComponentsClientHttpResponse extends AbstractClientHttpResponse {

    private final HttpResponse httpResponse;

    @Nullable
    private HttpHeaders headers;

    HttpComponentsClientHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    @Override
    public int getRawStatusCode() throws IOException {
        return this.httpResponse.getStatusLine().getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        return this.httpResponse.getStatusLine().getReasonPhrase();
    }

    @Override
    public HttpHeaders getHeaders() {
        if (this.headers == null) {
            this.headers = new HttpHeaders();
            for (Header header : this.httpResponse.getAllHeaders()) {
                this.headers.add(header.getName(), header.getValue());
            }
        }
        return this.headers;
    }

    @Override
    public InputStream getBody() throws IOException {
        HttpEntity entity = this.httpResponse.getEntity();
        return (entity != null ? entity.getContent() : StreamUtils.emptyInput());
    }

    @Override
    public void close() {
        // Release underlying connection back to the connection manager
        try {
            try {
                // Attempt to keep connection alive by consuming its remaining content
                EntityUtils.consume(this.httpResponse.getEntity());
            } finally {
                if (this.httpResponse instanceof Closeable) {
                    ((Closeable) this.httpResponse).close();
                }
            }
        } catch (IOException ex) {
            // Ignore exception on close...
        }
    }

}
