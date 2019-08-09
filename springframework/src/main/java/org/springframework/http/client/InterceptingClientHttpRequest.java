package org.springframework.http.client;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;

class InterceptingClientHttpRequest extends AbstractBufferingClientHttpRequest {

    private final ClientHttpRequestFactory requestFactory;

    private final List<ClientHttpRequestInterceptor> interceptors;

    private HttpMethod method;

    private URI uri;

    protected InterceptingClientHttpRequest(ClientHttpRequestFactory requestFactory, List<ClientHttpRequestInterceptor> interceptors, URI uri, HttpMethod method) {
        this.requestFactory = requestFactory;
        this.interceptors = interceptors;
        this.method = method;
        this.uri = uri;
    }

    @Override
    public HttpMethod getMethod() {
        return this.method;
    }

    @Override
    public String getMethodValue() {
        return this.method.name();
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    protected final ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput) throws IOException {
        InterceptingRequestExecution requestExecution = new InterceptingRequestExecution();
        return requestExecution.execute(this, bufferedOutput);
    }

    private class InterceptingRequestExecution implements ClientHttpRequestExecution {

        private final Iterator<ClientHttpRequestInterceptor> iterator;

        public InterceptingRequestExecution() {
            this.iterator = interceptors.iterator();
        }

        @Override
        public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
            if (this.iterator.hasNext()) {
                ClientHttpRequestInterceptor nextInterceptor = this.iterator.next();
                return nextInterceptor.intercept(request, body, this);
            } else {
                HttpMethod method = request.getMethod();
                Assert.state(method != null, "No standard HTTP method");
                ClientHttpRequest delegate = requestFactory.createRequest(request.getURI(), method);
                request.getHeaders().forEach((key, value) -> delegate.getHeaders().addAll(key, value));
                if (body.length > 0) {
                    if (delegate instanceof StreamingHttpOutputMessage) {
                        StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) delegate;
                        streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(body, outputStream));
                    } else {
                        StreamUtils.copy(body, delegate.getBody());
                    }
                }
                return delegate.execute();
            }
        }

    }

}
