package org.springframework.http.server;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpRequest;
import org.springframework.lang.Nullable;

import java.net.InetSocketAddress;
import java.security.Principal;

public interface ServerHttpRequest extends HttpRequest, HttpInputMessage {

    @Nullable
    Principal getPrincipal();

    InetSocketAddress getLocalAddress();

    InetSocketAddress getRemoteAddress();

    ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response);

}
