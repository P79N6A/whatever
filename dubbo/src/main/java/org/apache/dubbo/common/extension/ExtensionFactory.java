package org.apache.dubbo.common.extension;

@SPI
public interface ExtensionFactory {

    <T> T getExtension(Class<T> type, String name);

}
