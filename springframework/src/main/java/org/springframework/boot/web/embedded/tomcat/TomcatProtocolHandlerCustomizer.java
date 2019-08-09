package org.springframework.boot.web.embedded.tomcat;

import org.apache.coyote.ProtocolHandler;

@FunctionalInterface
public interface TomcatProtocolHandlerCustomizer<T extends ProtocolHandler> {

    void customize(T protocolHandler);

}
