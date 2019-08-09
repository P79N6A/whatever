package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;

@Deprecated
public interface ExtensionRegistryInitializer {

    void initializeExtensionRegistry(ExtensionRegistry registry);

}
