package org.springframework.http.client;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

public class HttpComponentsClientHttpRequestFactory implements ClientHttpRequestFactory, DisposableBean {

    private HttpClient httpClient;

    @Nullable
    private RequestConfig requestConfig;

    private boolean bufferRequestBody = true;

    public HttpComponentsClientHttpRequestFactory() {
        this.httpClient = HttpClients.createSystem();
    }

    public HttpComponentsClientHttpRequestFactory(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setHttpClient(HttpClient httpClient) {
        Assert.notNull(httpClient, "HttpClient must not be null");
        this.httpClient = httpClient;
    }

    public HttpClient getHttpClient() {
        return this.httpClient;
    }

    public void setConnectTimeout(int timeout) {
        Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
        this.requestConfig = requestConfigBuilder().setConnectTimeout(timeout).build();
    }

    public void setConnectionRequestTimeout(int connectionRequestTimeout) {
        this.requestConfig = requestConfigBuilder().setConnectionRequestTimeout(connectionRequestTimeout).build();
    }

    public void setReadTimeout(int timeout) {
        Assert.isTrue(timeout >= 0, "Timeout must be a non-negative value");
        this.requestConfig = requestConfigBuilder().setSocketTimeout(timeout).build();
    }

    public void setBufferRequestBody(boolean bufferRequestBody) {
        this.bufferRequestBody = bufferRequestBody;
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        HttpClient client = getHttpClient();
        HttpUriRequest httpRequest = createHttpUriRequest(httpMethod, uri);
        postProcessHttpRequest(httpRequest);
        HttpContext context = createHttpContext(httpMethod, uri);
        if (context == null) {
            context = HttpClientContext.create();
        }
        // Request configuration not set in the context
        if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
            // Use request configuration given by the user, when available
            RequestConfig config = null;
            if (httpRequest instanceof Configurable) {
                config = ((Configurable) httpRequest).getConfig();
            }
            if (config == null) {
                config = createRequestConfig(client);
            }
            if (config != null) {
                context.setAttribute(HttpClientContext.REQUEST_CONFIG, config);
            }
        }
        if (this.bufferRequestBody) {
            return new HttpComponentsClientHttpRequest(client, httpRequest, context);
        } else {
            return new HttpComponentsStreamingClientHttpRequest(client, httpRequest, context);
        }
    }

    private RequestConfig.Builder requestConfigBuilder() {
        return (this.requestConfig != null ? RequestConfig.copy(this.requestConfig) : RequestConfig.custom());
    }

    @Nullable
    protected RequestConfig createRequestConfig(Object client) {
        if (client instanceof Configurable) {
            RequestConfig clientRequestConfig = ((Configurable) client).getConfig();
            return mergeRequestConfig(clientRequestConfig);
        }
        return this.requestConfig;
    }

    protected RequestConfig mergeRequestConfig(RequestConfig clientConfig) {
        if (this.requestConfig == null) {  // nothing to merge
            return clientConfig;
        }
        RequestConfig.Builder builder = RequestConfig.copy(clientConfig);
        int connectTimeout = this.requestConfig.getConnectTimeout();
        if (connectTimeout >= 0) {
            builder.setConnectTimeout(connectTimeout);
        }
        int connectionRequestTimeout = this.requestConfig.getConnectionRequestTimeout();
        if (connectionRequestTimeout >= 0) {
            builder.setConnectionRequestTimeout(connectionRequestTimeout);
        }
        int socketTimeout = this.requestConfig.getSocketTimeout();
        if (socketTimeout >= 0) {
            builder.setSocketTimeout(socketTimeout);
        }
        return builder.build();
    }

    protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
        switch (httpMethod) {
            case GET:
                return new HttpGet(uri);
            case HEAD:
                return new HttpHead(uri);
            case POST:
                return new HttpPost(uri);
            case PUT:
                return new HttpPut(uri);
            case PATCH:
                return new HttpPatch(uri);
            case DELETE:
                return new HttpDelete(uri);
            case OPTIONS:
                return new HttpOptions(uri);
            case TRACE:
                return new HttpTrace(uri);
            default:
                throw new IllegalArgumentException("Invalid HTTP method: " + httpMethod);
        }
    }

    protected void postProcessHttpRequest(HttpUriRequest request) {
    }

    @Nullable
    protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
        return null;
    }

    @Override
    public void destroy() throws Exception {
        HttpClient httpClient = getHttpClient();
        if (httpClient instanceof Closeable) {
            ((Closeable) httpClient).close();
        }
    }

    private static class HttpDelete extends HttpEntityEnclosingRequestBase {

        public HttpDelete(URI uri) {
            super();
            setURI(uri);
        }

        @Override
        public String getMethod() {
            return "DELETE";
        }

    }

}
