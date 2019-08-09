package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.Context;

@FunctionalInterface
public interface TomcatContextCustomizer {

    void customize(Context context);

}
