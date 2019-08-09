package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Map;

public interface UriBuilder {

    UriBuilder scheme(@Nullable String scheme);

    UriBuilder userInfo(@Nullable String userInfo);

    UriBuilder host(@Nullable String host);

    UriBuilder port(int port);

    UriBuilder port(@Nullable String port);

    UriBuilder path(String path);

    UriBuilder replacePath(@Nullable String path);

    UriBuilder pathSegment(String... pathSegments) throws IllegalArgumentException;

    UriBuilder query(String query);

    UriBuilder replaceQuery(@Nullable String query);

    UriBuilder queryParam(String name, Object... values);

    UriBuilder queryParams(MultiValueMap<String, String> params);

    UriBuilder replaceQueryParam(String name, Object... values);

    UriBuilder replaceQueryParams(MultiValueMap<String, String> params);

    UriBuilder fragment(@Nullable String fragment);

    URI build(Object... uriVariables);

    URI build(Map<String, ?> uriVariables);

}
