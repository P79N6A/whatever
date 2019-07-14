package org.apache.dubbo.rpc.protocol;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.listener.ListenerExporterWrapper;
import org.apache.dubbo.rpc.listener.ListenerInvokerWrapper;

import java.util.Collections;

import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RpcConstants.EXPORTER_LISTENER_KEY;
import static org.apache.dubbo.common.constants.RpcConstants.INVOKER_LISTENER_KEY;

public class ProtocolListenerWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolListenerWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {

        // registry直接调用
        if (REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {
            return protocol.export(invoker);
        }
        return new ListenerExporterWrapper<T>(protocol.export(invoker), Collections.unmodifiableList(ExtensionLoader.getExtensionLoader(ExporterListener.class).getActivateExtension(invoker.getUrl(), EXPORTER_LISTENER_KEY)));
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {

        // registry直接调用
        if (REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        return new ListenerInvokerWrapper<T>(protocol.refer(type, url), Collections.unmodifiableList(ExtensionLoader.getExtensionLoader(InvokerListener.class).getActivateExtension(url, INVOKER_LISTENER_KEY)));
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

}
