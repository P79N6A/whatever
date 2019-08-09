package org.springframework.http.server.reactive;

import org.springframework.http.HttpStatus;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

public interface ServerHttpResponse extends ReactiveHttpOutputMessage {

    boolean setStatusCode(@Nullable HttpStatus status);

    @Nullable
    HttpStatus getStatusCode();

    MultiValueMap<String, ResponseCookie> getCookies();

    void addCookie(ResponseCookie cookie);

}
