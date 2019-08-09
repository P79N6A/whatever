package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DefaultUriBuilderFactory implements UriBuilderFactory {

    public enum EncodingMode {

        TEMPLATE_AND_VALUES,

        VALUES_ONLY,

        URI_COMPONENT,

        NONE
    }

    @Nullable
    private final UriComponentsBuilder baseUri;

    private EncodingMode encodingMode = EncodingMode.TEMPLATE_AND_VALUES;

    private final Map<String, Object> defaultUriVariables = new HashMap<>();

    private boolean parsePath = true;

    public DefaultUriBuilderFactory() {
        this.baseUri = null;
    }

    public DefaultUriBuilderFactory(String baseUriTemplate) {
        this.baseUri = UriComponentsBuilder.fromUriString(baseUriTemplate);
    }

    public DefaultUriBuilderFactory(UriComponentsBuilder baseUri) {
        this.baseUri = baseUri;
    }

    public void setEncodingMode(EncodingMode encodingMode) {
        this.encodingMode = encodingMode;
    }

    public EncodingMode getEncodingMode() {
        return this.encodingMode;
    }

    public void setDefaultUriVariables(@Nullable Map<String, ?> defaultUriVariables) {
        this.defaultUriVariables.clear();
        if (defaultUriVariables != null) {
            this.defaultUriVariables.putAll(defaultUriVariables);
        }
    }

    public Map<String, ?> getDefaultUriVariables() {
        return Collections.unmodifiableMap(this.defaultUriVariables);
    }

    public void setParsePath(boolean parsePath) {
        this.parsePath = parsePath;
    }

    public boolean shouldParsePath() {
        return this.parsePath;
    }
    // UriTemplateHandler

    public URI expand(String uriTemplate, Map<String, ?> uriVars) {
        return uriString(uriTemplate).build(uriVars);
    }

    public URI expand(String uriTemplate, Object... uriVars) {
        return uriString(uriTemplate).build(uriVars);
    }
    // UriBuilderFactory

    public UriBuilder uriString(String uriTemplate) {
        return new DefaultUriBuilder(uriTemplate);
    }

    @Override
    public UriBuilder builder() {
        return new DefaultUriBuilder("");
    }

    private class DefaultUriBuilder implements UriBuilder {

        private final UriComponentsBuilder uriComponentsBuilder;

        public DefaultUriBuilder(String uriTemplate) {
            this.uriComponentsBuilder = initUriComponentsBuilder(uriTemplate);
        }

        private UriComponentsBuilder initUriComponentsBuilder(String uriTemplate) {
            UriComponentsBuilder result;
            if (!StringUtils.hasLength(uriTemplate)) {
                result = (baseUri != null ? baseUri.cloneBuilder() : UriComponentsBuilder.newInstance());
            } else if (baseUri != null) {
                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(uriTemplate);
                UriComponents uri = builder.build();
                result = (uri.getHost() == null ? baseUri.cloneBuilder().uriComponents(uri) : builder);
            } else {
                result = UriComponentsBuilder.fromUriString(uriTemplate);
            }
            if (encodingMode.equals(EncodingMode.TEMPLATE_AND_VALUES)) {
                result.encode();
            }
            parsePathIfNecessary(result);
            return result;
        }

        private void parsePathIfNecessary(UriComponentsBuilder result) {
            if (parsePath && encodingMode.equals(EncodingMode.URI_COMPONENT)) {
                UriComponents uric = result.build();
                String path = uric.getPath();
                result.replacePath(null);
                for (String segment : uric.getPathSegments()) {
                    result.pathSegment(segment);
                }
                if (path != null && path.endsWith("/")) {
                    result.path("/");
                }
            }
        }

        @Override
        public DefaultUriBuilder scheme(@Nullable String scheme) {
            this.uriComponentsBuilder.scheme(scheme);
            return this;
        }

        @Override
        public DefaultUriBuilder userInfo(@Nullable String userInfo) {
            this.uriComponentsBuilder.userInfo(userInfo);
            return this;
        }

        @Override
        public DefaultUriBuilder host(@Nullable String host) {
            this.uriComponentsBuilder.host(host);
            return this;
        }

        @Override
        public DefaultUriBuilder port(int port) {
            this.uriComponentsBuilder.port(port);
            return this;
        }

        @Override
        public DefaultUriBuilder port(@Nullable String port) {
            this.uriComponentsBuilder.port(port);
            return this;
        }

        @Override
        public DefaultUriBuilder path(String path) {
            this.uriComponentsBuilder.path(path);
            return this;
        }

        @Override
        public DefaultUriBuilder replacePath(@Nullable String path) {
            this.uriComponentsBuilder.replacePath(path);
            return this;
        }

        @Override
        public DefaultUriBuilder pathSegment(String... pathSegments) {
            this.uriComponentsBuilder.pathSegment(pathSegments);
            return this;
        }

        @Override
        public DefaultUriBuilder query(String query) {
            this.uriComponentsBuilder.query(query);
            return this;
        }

        @Override
        public DefaultUriBuilder replaceQuery(@Nullable String query) {
            this.uriComponentsBuilder.replaceQuery(query);
            return this;
        }

        @Override
        public DefaultUriBuilder queryParam(String name, Object... values) {
            this.uriComponentsBuilder.queryParam(name, values);
            return this;
        }

        @Override
        public DefaultUriBuilder replaceQueryParam(String name, Object... values) {
            this.uriComponentsBuilder.replaceQueryParam(name, values);
            return this;
        }

        @Override
        public DefaultUriBuilder queryParams(MultiValueMap<String, String> params) {
            this.uriComponentsBuilder.queryParams(params);
            return this;
        }

        @Override
        public DefaultUriBuilder replaceQueryParams(MultiValueMap<String, String> params) {
            this.uriComponentsBuilder.replaceQueryParams(params);
            return this;
        }

        @Override
        public DefaultUriBuilder fragment(@Nullable String fragment) {
            this.uriComponentsBuilder.fragment(fragment);
            return this;
        }

        @Override
        public URI build(Map<String, ?> uriVars) {
            if (!defaultUriVariables.isEmpty()) {
                Map<String, Object> map = new HashMap<>();
                map.putAll(defaultUriVariables);
                map.putAll(uriVars);
                uriVars = map;
            }
            if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
                uriVars = UriUtils.encodeUriVariables(uriVars);
            }
            UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
            return createUri(uric);
        }

        @Override
        public URI build(Object... uriVars) {
            if (ObjectUtils.isEmpty(uriVars) && !defaultUriVariables.isEmpty()) {
                return build(Collections.emptyMap());
            }
            if (encodingMode.equals(EncodingMode.VALUES_ONLY)) {
                uriVars = UriUtils.encodeUriVariables(uriVars);
            }
            UriComponents uric = this.uriComponentsBuilder.build().expand(uriVars);
            return createUri(uric);
        }

        private URI createUri(UriComponents uric) {
            if (encodingMode.equals(EncodingMode.URI_COMPONENT)) {
                uric = uric.encode();
            }
            return URI.create(uric.toString());
        }

    }

}
