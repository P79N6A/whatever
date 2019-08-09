package org.springframework.web.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.util.HierarchicalUriComponents.PathComponent;
import org.springframework.web.util.UriComponents.UriTemplateVariables;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriComponentsBuilder implements UriBuilder, Cloneable {

    private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

    private static final String SCHEME_PATTERN = "([^:/?#]+):";

    private static final String HTTP_PATTERN = "(?i)(http|https):";

    private static final String USERINFO_PATTERN = "([^@\\[/?#]*)";

    private static final String HOST_IPV4_PATTERN = "[^\\[/?#:]*";

    private static final String HOST_IPV6_PATTERN = "\\[[\\p{XDigit}\\:\\.]*[%\\p{Alnum}]*\\]";

    private static final String HOST_PATTERN = "(" + HOST_IPV6_PATTERN + "|" + HOST_IPV4_PATTERN + ")";

    private static final String PORT_PATTERN = "(\\d*(?:\\{[^/]+?\\})?)";

    private static final String PATH_PATTERN = "([^?#]*)";

    private static final String QUERY_PATTERN = "([^#]*)";

    private static final String LAST_PATTERN = "(.*)";

    // Regex patterns that matches URIs. See RFC 3986, appendix B
    private static final Pattern URI_PATTERN = Pattern.compile("^(" + SCHEME_PATTERN + ")?" + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" + PATH_PATTERN + "(\\?" + QUERY_PATTERN + ")?" + "(#" + LAST_PATTERN + ")?");

    private static final Pattern HTTP_URL_PATTERN = Pattern.compile("^" + HTTP_PATTERN + "(//(" + USERINFO_PATTERN + "@)?" + HOST_PATTERN + "(:" + PORT_PATTERN + ")?" + ")?" + PATH_PATTERN + "(\\?" + LAST_PATTERN + ")?");

    private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?");

    private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?");

    @Nullable
    private String scheme;

    @Nullable
    private String ssp;

    @Nullable
    private String userInfo;

    @Nullable
    private String host;

    @Nullable
    private String port;

    private CompositePathComponentBuilder pathBuilder;

    private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

    @Nullable
    private String fragment;

    private final Map<String, Object> uriVariables = new HashMap<>(4);

    private boolean encodeTemplate;

    private Charset charset = StandardCharsets.UTF_8;

    protected UriComponentsBuilder() {
        this.pathBuilder = new CompositePathComponentBuilder();
    }

    protected UriComponentsBuilder(UriComponentsBuilder other) {
        this.scheme = other.scheme;
        this.ssp = other.ssp;
        this.userInfo = other.userInfo;
        this.host = other.host;
        this.port = other.port;
        this.pathBuilder = other.pathBuilder.cloneBuilder();
        this.queryParams.putAll(other.queryParams);
        this.fragment = other.fragment;
        this.encodeTemplate = other.encodeTemplate;
        this.charset = other.charset;
    }
    // Factory methods

    public static UriComponentsBuilder newInstance() {
        return new UriComponentsBuilder();
    }

    public static UriComponentsBuilder fromPath(String path) {
        UriComponentsBuilder builder = new UriComponentsBuilder();
        builder.path(path);
        return builder;
    }

    public static UriComponentsBuilder fromUri(URI uri) {
        UriComponentsBuilder builder = new UriComponentsBuilder();
        builder.uri(uri);
        return builder;
    }

    public static UriComponentsBuilder fromUriString(String uri) {
        Assert.notNull(uri, "URI must not be null");
        Matcher matcher = URI_PATTERN.matcher(uri);
        if (matcher.matches()) {
            UriComponentsBuilder builder = new UriComponentsBuilder();
            String scheme = matcher.group(2);
            String userInfo = matcher.group(5);
            String host = matcher.group(6);
            String port = matcher.group(8);
            String path = matcher.group(9);
            String query = matcher.group(11);
            String fragment = matcher.group(13);
            boolean opaque = false;
            if (StringUtils.hasLength(scheme)) {
                String rest = uri.substring(scheme.length());
                if (!rest.startsWith(":/")) {
                    opaque = true;
                }
            }
            builder.scheme(scheme);
            if (opaque) {
                String ssp = uri.substring(scheme.length()).substring(1);
                if (StringUtils.hasLength(fragment)) {
                    ssp = ssp.substring(0, ssp.length() - (fragment.length() + 1));
                }
                builder.schemeSpecificPart(ssp);
            } else {
                builder.userInfo(userInfo);
                builder.host(host);
                if (StringUtils.hasLength(port)) {
                    builder.port(port);
                }
                builder.path(path);
                builder.query(query);
            }
            if (StringUtils.hasText(fragment)) {
                builder.fragment(fragment);
            }
            return builder;
        } else {
            throw new IllegalArgumentException("[" + uri + "] is not a valid URI");
        }
    }

    public static UriComponentsBuilder fromHttpUrl(String httpUrl) {
        Assert.notNull(httpUrl, "HTTP URL must not be null");
        Matcher matcher = HTTP_URL_PATTERN.matcher(httpUrl);
        if (matcher.matches()) {
            UriComponentsBuilder builder = new UriComponentsBuilder();
            String scheme = matcher.group(1);
            builder.scheme(scheme != null ? scheme.toLowerCase() : null);
            builder.userInfo(matcher.group(4));
            String host = matcher.group(5);
            if (StringUtils.hasLength(scheme) && !StringUtils.hasLength(host)) {
                throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
            }
            builder.host(host);
            String port = matcher.group(7);
            if (StringUtils.hasLength(port)) {
                builder.port(port);
            }
            builder.path(matcher.group(8));
            builder.query(matcher.group(10));
            return builder;
        } else {
            throw new IllegalArgumentException("[" + httpUrl + "] is not a valid HTTP URL");
        }
    }

    public static UriComponentsBuilder fromHttpRequest(HttpRequest request) {
        return fromUri(request.getURI()).adaptFromForwardedHeaders(request.getHeaders());
    }

    public static UriComponentsBuilder fromOriginHeader(String origin) {
        Matcher matcher = URI_PATTERN.matcher(origin);
        if (matcher.matches()) {
            UriComponentsBuilder builder = new UriComponentsBuilder();
            String scheme = matcher.group(2);
            String host = matcher.group(6);
            String port = matcher.group(8);
            if (StringUtils.hasLength(scheme)) {
                builder.scheme(scheme);
            }
            builder.host(host);
            if (StringUtils.hasLength(port)) {
                builder.port(port);
            }
            return builder;
        } else {
            throw new IllegalArgumentException("[" + origin + "] is not a valid \"Origin\" header value");
        }
    }
    // Encode methods

    public final UriComponentsBuilder encode() {
        return encode(StandardCharsets.UTF_8);
    }

    public UriComponentsBuilder encode(Charset charset) {
        this.encodeTemplate = true;
        this.charset = charset;
        return this;
    }
    // Build methods

    public UriComponents build() {
        return build(false);
    }

    public UriComponents build(boolean encoded) {
        return buildInternal(encoded ? EncodingHint.FULLY_ENCODED : this.encodeTemplate ? EncodingHint.ENCODE_TEMPLATE : EncodingHint.NONE);
    }

    private UriComponents buildInternal(EncodingHint hint) {
        UriComponents result;
        if (this.ssp != null) {
            result = new OpaqueUriComponents(this.scheme, this.ssp, this.fragment);
        } else {
            HierarchicalUriComponents uric = new HierarchicalUriComponents(this.scheme, this.fragment, this.userInfo, this.host, this.port, this.pathBuilder.build(), this.queryParams, hint == EncodingHint.FULLY_ENCODED);
            result = hint == EncodingHint.ENCODE_TEMPLATE ? uric.encodeTemplate(this.charset) : uric;
        }
        if (!this.uriVariables.isEmpty()) {
            result = result.expand(name -> this.uriVariables.getOrDefault(name, UriTemplateVariables.SKIP_VALUE));
        }
        return result;
    }

    public UriComponents buildAndExpand(Map<String, ?> uriVariables) {
        return build().expand(uriVariables);
    }

    public UriComponents buildAndExpand(Object... uriVariableValues) {
        return build().expand(uriVariableValues);
    }

    @Override
    public URI build(Object... uriVariables) {
        return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
    }

    @Override
    public URI build(Map<String, ?> uriVariables) {
        return buildInternal(EncodingHint.ENCODE_TEMPLATE).expand(uriVariables).toUri();
    }

    public String toUriString() {
        return this.uriVariables.isEmpty() ? build().encode().toUriString() : buildInternal(EncodingHint.ENCODE_TEMPLATE).toUriString();
    }
    // Instance methods

    public UriComponentsBuilder uri(URI uri) {
        Assert.notNull(uri, "URI must not be null");
        this.scheme = uri.getScheme();
        if (uri.isOpaque()) {
            this.ssp = uri.getRawSchemeSpecificPart();
            resetHierarchicalComponents();
        } else {
            if (uri.getRawUserInfo() != null) {
                this.userInfo = uri.getRawUserInfo();
            }
            if (uri.getHost() != null) {
                this.host = uri.getHost();
            }
            if (uri.getPort() != -1) {
                this.port = String.valueOf(uri.getPort());
            }
            if (StringUtils.hasLength(uri.getRawPath())) {
                this.pathBuilder = new CompositePathComponentBuilder();
                this.pathBuilder.addPath(uri.getRawPath());
            }
            if (StringUtils.hasLength(uri.getRawQuery())) {
                this.queryParams.clear();
                query(uri.getRawQuery());
            }
            resetSchemeSpecificPart();
        }
        if (uri.getRawFragment() != null) {
            this.fragment = uri.getRawFragment();
        }
        return this;
    }

    public UriComponentsBuilder uriComponents(UriComponents uriComponents) {
        Assert.notNull(uriComponents, "UriComponents must not be null");
        uriComponents.copyToUriComponentsBuilder(this);
        return this;
    }

    @Override
    public UriComponentsBuilder scheme(@Nullable String scheme) {
        this.scheme = scheme;
        return this;
    }

    public UriComponentsBuilder schemeSpecificPart(String ssp) {
        this.ssp = ssp;
        resetHierarchicalComponents();
        return this;
    }

    @Override
    public UriComponentsBuilder userInfo(@Nullable String userInfo) {
        this.userInfo = userInfo;
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder host(@Nullable String host) {
        this.host = host;
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder port(int port) {
        Assert.isTrue(port >= -1, "Port must be >= -1");
        this.port = String.valueOf(port);
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder port(@Nullable String port) {
        this.port = port;
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder path(String path) {
        this.pathBuilder.addPath(path);
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder pathSegment(String... pathSegments) throws IllegalArgumentException {
        this.pathBuilder.addPathSegments(pathSegments);
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder replacePath(@Nullable String path) {
        this.pathBuilder = new CompositePathComponentBuilder();
        if (path != null) {
            this.pathBuilder.addPath(path);
        }
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder query(@Nullable String query) {
        if (query != null) {
            Matcher matcher = QUERY_PARAM_PATTERN.matcher(query);
            while (matcher.find()) {
                String name = matcher.group(1);
                String eq = matcher.group(2);
                String value = matcher.group(3);
                queryParam(name, (value != null ? value : (StringUtils.hasLength(eq) ? "" : null)));
            }
        } else {
            this.queryParams.clear();
        }
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder replaceQuery(@Nullable String query) {
        this.queryParams.clear();
        if (query != null) {
            query(query);
        }
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder queryParam(String name, Object... values) {
        Assert.notNull(name, "Name must not be null");
        if (!ObjectUtils.isEmpty(values)) {
            for (Object value : values) {
                String valueAsString = (value != null ? value.toString() : null);
                this.queryParams.add(name, valueAsString);
            }
        } else {
            this.queryParams.add(name, null);
        }
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder queryParams(@Nullable MultiValueMap<String, String> params) {
        if (params != null) {
            this.queryParams.addAll(params);
        }
        return this;
    }

    @Override
    public UriComponentsBuilder replaceQueryParam(String name, Object... values) {
        Assert.notNull(name, "Name must not be null");
        this.queryParams.remove(name);
        if (!ObjectUtils.isEmpty(values)) {
            queryParam(name, values);
        }
        resetSchemeSpecificPart();
        return this;
    }

    @Override
    public UriComponentsBuilder replaceQueryParams(@Nullable MultiValueMap<String, String> params) {
        this.queryParams.clear();
        if (params != null) {
            this.queryParams.putAll(params);
        }
        return this;
    }

    @Override
    public UriComponentsBuilder fragment(@Nullable String fragment) {
        if (fragment != null) {
            Assert.hasLength(fragment, "Fragment must not be empty");
            this.fragment = fragment;
        } else {
            this.fragment = null;
        }
        return this;
    }

    public UriComponentsBuilder uriVariables(Map<String, Object> uriVariables) {
        this.uriVariables.putAll(uriVariables);
        return this;
    }

    UriComponentsBuilder adaptFromForwardedHeaders(HttpHeaders headers) {
        try {
            String forwardedHeader = headers.getFirst("Forwarded");
            if (StringUtils.hasText(forwardedHeader)) {
                String forwardedToUse = StringUtils.tokenizeToStringArray(forwardedHeader, ",")[0];
                Matcher matcher = FORWARDED_PROTO_PATTERN.matcher(forwardedToUse);
                if (matcher.find()) {
                    scheme(matcher.group(1).trim());
                    port(null);
                } else if (isForwardedSslOn(headers)) {
                    scheme("https");
                    port(null);
                }
                matcher = FORWARDED_HOST_PATTERN.matcher(forwardedToUse);
                if (matcher.find()) {
                    adaptForwardedHost(matcher.group(1).trim());
                }
            } else {
                String protocolHeader = headers.getFirst("X-Forwarded-Proto");
                if (StringUtils.hasText(protocolHeader)) {
                    scheme(StringUtils.tokenizeToStringArray(protocolHeader, ",")[0]);
                    port(null);
                } else if (isForwardedSslOn(headers)) {
                    scheme("https");
                    port(null);
                }
                String hostHeader = headers.getFirst("X-Forwarded-Host");
                if (StringUtils.hasText(hostHeader)) {
                    adaptForwardedHost(StringUtils.tokenizeToStringArray(hostHeader, ",")[0]);
                }
                String portHeader = headers.getFirst("X-Forwarded-Port");
                if (StringUtils.hasText(portHeader)) {
                    port(Integer.parseInt(StringUtils.tokenizeToStringArray(portHeader, ",")[0]));
                }
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Failed to parse a port from \"forwarded\"-type headers. " + "If not behind a trusted proxy, consider using ForwardedHeaderFilter " + "with the removeOnly=true. Request headers: " + headers);
        }
        if (this.scheme != null && ((this.scheme.equals("http") && "80".equals(this.port)) || (this.scheme.equals("https") && "443".equals(this.port)))) {
            port(null);
        }
        return this;
    }

    private boolean isForwardedSslOn(HttpHeaders headers) {
        String forwardedSsl = headers.getFirst("X-Forwarded-Ssl");
        return StringUtils.hasText(forwardedSsl) && forwardedSsl.equalsIgnoreCase("on");
    }

    private void adaptForwardedHost(String hostToUse) {
        int portSeparatorIdx = hostToUse.lastIndexOf(':');
        if (portSeparatorIdx > hostToUse.lastIndexOf(']')) {
            host(hostToUse.substring(0, portSeparatorIdx));
            port(Integer.parseInt(hostToUse.substring(portSeparatorIdx + 1)));
        } else {
            host(hostToUse);
            port(null);
        }
    }

    private void resetHierarchicalComponents() {
        this.userInfo = null;
        this.host = null;
        this.port = null;
        this.pathBuilder = new CompositePathComponentBuilder();
        this.queryParams.clear();
    }

    private void resetSchemeSpecificPart() {
        this.ssp = null;
    }

    @Override
    public Object clone() {
        return cloneBuilder();
    }

    public UriComponentsBuilder cloneBuilder() {
        return new UriComponentsBuilder(this);
    }

    private interface PathComponentBuilder {

        @Nullable
        PathComponent build();

        PathComponentBuilder cloneBuilder();

    }

    private static class CompositePathComponentBuilder implements PathComponentBuilder {

        private final LinkedList<PathComponentBuilder> builders = new LinkedList<>();

        public void addPathSegments(String... pathSegments) {
            if (!ObjectUtils.isEmpty(pathSegments)) {
                PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
                FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
                if (psBuilder == null) {
                    psBuilder = new PathSegmentComponentBuilder();
                    this.builders.add(psBuilder);
                    if (fpBuilder != null) {
                        fpBuilder.removeTrailingSlash();
                    }
                }
                psBuilder.append(pathSegments);
            }
        }

        public void addPath(String path) {
            if (StringUtils.hasText(path)) {
                PathSegmentComponentBuilder psBuilder = getLastBuilder(PathSegmentComponentBuilder.class);
                FullPathComponentBuilder fpBuilder = getLastBuilder(FullPathComponentBuilder.class);
                if (psBuilder != null) {
                    path = (path.startsWith("/") ? path : "/" + path);
                }
                if (fpBuilder == null) {
                    fpBuilder = new FullPathComponentBuilder();
                    this.builders.add(fpBuilder);
                }
                fpBuilder.append(path);
            }
        }

        @SuppressWarnings("unchecked")
        @Nullable
        private <T> T getLastBuilder(Class<T> builderClass) {
            if (!this.builders.isEmpty()) {
                PathComponentBuilder last = this.builders.getLast();
                if (builderClass.isInstance(last)) {
                    return (T) last;
                }
            }
            return null;
        }

        @Override
        public PathComponent build() {
            int size = this.builders.size();
            List<PathComponent> components = new ArrayList<>(size);
            for (PathComponentBuilder componentBuilder : this.builders) {
                PathComponent pathComponent = componentBuilder.build();
                if (pathComponent != null) {
                    components.add(pathComponent);
                }
            }
            if (components.isEmpty()) {
                return HierarchicalUriComponents.NULL_PATH_COMPONENT;
            }
            if (components.size() == 1) {
                return components.get(0);
            }
            return new HierarchicalUriComponents.PathComponentComposite(components);
        }

        @Override
        public CompositePathComponentBuilder cloneBuilder() {
            CompositePathComponentBuilder compositeBuilder = new CompositePathComponentBuilder();
            for (PathComponentBuilder builder : this.builders) {
                compositeBuilder.builders.add(builder.cloneBuilder());
            }
            return compositeBuilder;
        }

    }

    private static class FullPathComponentBuilder implements PathComponentBuilder {

        private final StringBuilder path = new StringBuilder();

        public void append(String path) {
            this.path.append(path);
        }

        @Override
        public PathComponent build() {
            if (this.path.length() == 0) {
                return null;
            }
            String path = this.path.toString();
            while (true) {
                int index = path.indexOf("//");
                if (index == -1) {
                    break;
                }
                path = path.substring(0, index) + path.substring(index + 1);
            }
            return new HierarchicalUriComponents.FullPathComponent(path);
        }

        public void removeTrailingSlash() {
            int index = this.path.length() - 1;
            if (this.path.charAt(index) == '/') {
                this.path.deleteCharAt(index);
            }
        }

        @Override
        public FullPathComponentBuilder cloneBuilder() {
            FullPathComponentBuilder builder = new FullPathComponentBuilder();
            builder.append(this.path.toString());
            return builder;
        }

    }

    private static class PathSegmentComponentBuilder implements PathComponentBuilder {

        private final List<String> pathSegments = new LinkedList<>();

        public void append(String... pathSegments) {
            for (String pathSegment : pathSegments) {
                if (StringUtils.hasText(pathSegment)) {
                    this.pathSegments.add(pathSegment);
                }
            }
        }

        @Override
        public PathComponent build() {
            return (this.pathSegments.isEmpty() ? null : new HierarchicalUriComponents.PathSegmentComponent(this.pathSegments));
        }

        @Override
        public PathSegmentComponentBuilder cloneBuilder() {
            PathSegmentComponentBuilder builder = new PathSegmentComponentBuilder();
            builder.pathSegments.addAll(this.pathSegments);
            return builder;
        }

    }

    private enum EncodingHint {ENCODE_TEMPLATE, FULLY_ENCODED, NONE}

}
