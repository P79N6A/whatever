package org.springframework.core.type.classreading;

import org.springframework.core.io.Resource;

import java.io.IOException;

public interface MetadataReaderFactory {

    MetadataReader getMetadataReader(String className) throws IOException;

    MetadataReader getMetadataReader(Resource resource) throws IOException;

}
