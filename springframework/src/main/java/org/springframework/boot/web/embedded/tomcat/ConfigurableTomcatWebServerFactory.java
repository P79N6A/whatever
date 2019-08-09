package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.Valve;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;

import java.io.File;
import java.nio.charset.Charset;

public interface ConfigurableTomcatWebServerFactory extends ConfigurableWebServerFactory {

    void setBaseDirectory(File baseDirectory);

    void setBackgroundProcessorDelay(int delay);

    void addEngineValves(Valve... engineValves);

    void addConnectorCustomizers(TomcatConnectorCustomizer... tomcatConnectorCustomizers);

    void addContextCustomizers(TomcatContextCustomizer... tomcatContextCustomizers);

    void addProtocolHandlerCustomizers(TomcatProtocolHandlerCustomizer<?>... tomcatProtocolHandlerCustomizers);

    void setUriEncoding(Charset uriEncoding);

}
