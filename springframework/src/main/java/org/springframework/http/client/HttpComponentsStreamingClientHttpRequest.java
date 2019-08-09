package org.springframework.http.client;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

final class HttpComponentsStreamingClientHttpRequest extends AbstractClientHttpRequest implements StreamingHttpOutputMessage {

    private final HttpClient httpClient;

    private final HttpUriRequest httpRequest;

    private final HttpContext httpContext;

    @Nullable
    private Body body;

    HttpComponentsStreamingClientHttpRequest(HttpClient client, HttpUriRequest request, HttpContext context) {
        this.httpClient = client;
        this.httpRequest = request;
        this.httpContext = context;
    }

    @Override
    public String getMethodValue() {
        return this.httpRequest.getMethod();
    }

    @Override
    public URI getURI() {
        return this.httpRequest.getURI();
    }

    @Override
    public void setBody(Body body) {
        assertNotExecuted();
        this.body = body;
    }

    @Override
    protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
        throw new UnsupportedOperationException("getBody not supported");
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
        HttpComponentsClientHttpRequest.addHeaders(this.httpRequest, headers);
        if (this.httpRequest instanceof HttpEntityEnclosingRequest && this.body != null) {
            HttpEntityEnclosingRequest entityEnclosingRequest = (HttpEntityEnclosingRequest) this.httpRequest;
            HttpEntity requestEntity = new StreamingHttpEntity(getHeaders(), this.body);
            entityEnclosingRequest.setEntity(requestEntity);
        }
        HttpResponse httpResponse = this.httpClient.execute(this.httpRequest, this.httpContext);
        return new HttpComponentsClientHttpResponse(httpResponse);
    }

    private static class StreamingHttpEntity implements HttpEntity {

        private final HttpHeaders headers;

        private final StreamingHttpOutputMessage.Body body;

        public StreamingHttpEntity(HttpHeaders headers, StreamingHttpOutputMessage.Body body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public boolean isRepeatable() {
            return false;
        }

        @Override
        public boolean isChunked() {
            return false;
        }

        @Override
        public long getContentLength() {
            return this.headers.getContentLength();
        }

        @Override
        @Nullable
        public Header getContentType() {
            MediaType contentType = this.headers.getContentType();
            return (contentType != null ? new BasicHeader("Content-Type", contentType.toString()) : null);
        }

        @Override
        @Nullable
        public Header getContentEncoding() {
            String contentEncoding = this.headers.getFirst("Content-Encoding");
            return (contentEncoding != null ? new BasicHeader("Content-Encoding", contentEncoding) : null);

        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            throw new IllegalStateException("No content available");
        }

        @Override
        public void writeTo(OutputStream outputStream) throws IOException {
            this.body.writeTo(outputStream);
        }

        @Override
        public boolean isStreaming() {
            return true;
        }

        @Override
        @Deprecated
        public void consumeContent() throws IOException {
            throw new UnsupportedOperationException();
        }

    }

}
