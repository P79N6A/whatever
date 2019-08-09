package org.springframework.http.server;

import org.springframework.lang.Nullable;

import java.net.URI;

public interface RequestPath extends PathContainer {

    PathContainer contextPath();

    PathContainer pathWithinApplication();

    RequestPath modifyContextPath(String contextPath);

    static RequestPath parse(URI uri, @Nullable String contextPath) {
        return new DefaultRequestPath(uri, contextPath);
    }

}
