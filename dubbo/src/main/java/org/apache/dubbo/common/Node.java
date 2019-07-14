package org.apache.dubbo.common;

public interface Node {

    URL getUrl();

    boolean isAvailable();

    void destroy();

}