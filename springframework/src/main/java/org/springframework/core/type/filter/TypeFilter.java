package org.springframework.core.type.filter;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;

@FunctionalInterface
public interface TypeFilter {

    boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException;

}
