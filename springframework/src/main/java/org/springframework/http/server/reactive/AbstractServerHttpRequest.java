package org.springframework.http.server.reactive;

import org.apache.commons.logging.Log;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpLogging;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractServerHttpRequest implements ServerHttpRequest {

    private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

    protected final Log logger = HttpLogging.forLogName(getClass());

    private final URI uri;

    private final RequestPath path;

    private final HttpHeaders headers;

    @Nullable
    private MultiValueMap<String, String> queryParams;

    @Nullable
    private MultiValueMap<String, HttpCookie> cookies;

    @Nullable
    private SslInfo sslInfo;

    @Nullable
    private String id;

    @Nullable
    private String logPrefix;

    public AbstractServerHttpRequest(URI uri, @Nullable String contextPath, HttpHeaders headers) {
        this.uri = uri;
        this.path = RequestPath.parse(uri, contextPath);
        this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
    }

    public String getId() {
        if (this.id == null) {
            this.id = initId();
            if (this.id == null) {
                this.id = ObjectUtils.getIdentityHexString(this);
            }
        }
        return this.id;
    }

    @Nullable
    protected String initId() {
        return null;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public RequestPath getPath() {
        return this.path;
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }

    @Override
    public MultiValueMap<String, String> getQueryParams() {
        if (this.queryParams == null) {
            this.queryParams = CollectionUtils.unmodifiableMultiValueMap(initQueryParams());
        }
        return this.queryParams;
    }

    protected MultiValueMap<String, String> initQueryParams() {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        String query = getURI().getRawQuery();
        if (query != null) {
            Matcher matcher = QUERY_PATTERN.matcher(query);
            while (matcher.find()) {
                String name = decodeQueryParam(matcher.group(1));
                String eq = matcher.group(2);
                String value = matcher.group(3);
                value = (value != null ? decodeQueryParam(value) : (StringUtils.hasLength(eq) ? "" : null));
                queryParams.add(name, value);
            }
        }
        return queryParams;
    }

    @SuppressWarnings("deprecation")
    private String decodeQueryParam(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn(getLogPrefix() + "Could not decode query value [" + value + "] as 'UTF-8'. " + "Falling back on default encoding: " + ex.getMessage());
            }
            return URLDecoder.decode(value);
        }
    }

    @Override
    public MultiValueMap<String, HttpCookie> getCookies() {
        if (this.cookies == null) {
            this.cookies = CollectionUtils.unmodifiableMultiValueMap(initCookies());
        }
        return this.cookies;
    }

    protected abstract MultiValueMap<String, HttpCookie> initCookies();

    @Nullable
    @Override
    public SslInfo getSslInfo() {
        if (this.sslInfo == null) {
            this.sslInfo = initSslInfo();
        }
        return this.sslInfo;
    }

    @Nullable
    protected abstract SslInfo initSslInfo();

    public abstract <T> T getNativeRequest();

    String getLogPrefix() {
        if (this.logPrefix == null) {
            this.logPrefix = "[" + getId() + "] ";
        }
        return this.logPrefix;
    }

}
