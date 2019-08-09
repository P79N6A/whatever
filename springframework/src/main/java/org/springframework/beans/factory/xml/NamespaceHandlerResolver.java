package org.springframework.beans.factory.xml;

import org.springframework.lang.Nullable;

@FunctionalInterface
public interface NamespaceHandlerResolver {

    @Nullable
    NamespaceHandler resolve(String namespaceUri);

}
