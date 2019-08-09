package org.springframework.core.type.filter;

import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;

public abstract class AbstractClassTestingTypeFilter implements TypeFilter {

    @Override
    public final boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        return match(metadataReader.getClassMetadata());
    }

    protected abstract boolean match(ClassMetadata metadata);

}
