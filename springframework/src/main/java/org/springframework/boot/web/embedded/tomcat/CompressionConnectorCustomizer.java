package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.UpgradeProtocol;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.springframework.boot.web.server.Compression;
import org.springframework.util.StringUtils;

class CompressionConnectorCustomizer implements TomcatConnectorCustomizer {

    private final Compression compression;

    CompressionConnectorCustomizer(Compression compression) {
        this.compression = compression;
    }

    @Override
    public void customize(Connector connector) {
        if (this.compression != null && this.compression.getEnabled()) {
            ProtocolHandler handler = connector.getProtocolHandler();
            if (handler instanceof AbstractHttp11Protocol) {
                customize((AbstractHttp11Protocol<?>) handler);
            }
            for (UpgradeProtocol upgradeProtocol : connector.findUpgradeProtocols()) {
                if (upgradeProtocol instanceof Http2Protocol) {
                    customize((Http2Protocol) upgradeProtocol);
                }
            }
        }
    }

    private void customize(Http2Protocol protocol) {
        Compression compression = this.compression;
        protocol.setCompression("on");
        protocol.setCompressionMinSize(getMinResponseSize(compression));
        protocol.setCompressibleMimeType(getMimeTypes(compression));
        if (this.compression.getExcludedUserAgents() != null) {
            protocol.setNoCompressionUserAgents(getExcludedUserAgents());
        }
    }

    private void customize(AbstractHttp11Protocol<?> protocol) {
        Compression compression = this.compression;
        protocol.setCompression("on");
        protocol.setCompressionMinSize(getMinResponseSize(compression));
        protocol.setCompressibleMimeType(getMimeTypes(compression));
        if (this.compression.getExcludedUserAgents() != null) {
            protocol.setNoCompressionUserAgents(getExcludedUserAgents());
        }
    }

    private int getMinResponseSize(Compression compression) {
        return (int) compression.getMinResponseSize().toBytes();
    }

    private String getMimeTypes(Compression compression) {
        return StringUtils.arrayToCommaDelimitedString(compression.getMimeTypes());
    }

    private String getExcludedUserAgents() {
        return StringUtils.arrayToCommaDelimitedString(this.compression.getExcludedUserAgents());
    }

}
