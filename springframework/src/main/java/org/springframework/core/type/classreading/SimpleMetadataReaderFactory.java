package org.springframework.core.type.classreading;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

public class SimpleMetadataReaderFactory implements MetadataReaderFactory {

    private final ResourceLoader resourceLoader;

    public SimpleMetadataReaderFactory() {
        this.resourceLoader = new DefaultResourceLoader();
    }

    public SimpleMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
        this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
    }

    public SimpleMetadataReaderFactory(@Nullable ClassLoader classLoader) {
        this.resourceLoader = (classLoader != null ? new DefaultResourceLoader(classLoader) : new DefaultResourceLoader());
    }

    public final ResourceLoader getResourceLoader() {
        return this.resourceLoader;
    }

    @Override
    public MetadataReader getMetadataReader(String className) throws IOException {
        try {
            String resourcePath = ResourceLoader.CLASSPATH_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(className) + ClassUtils.CLASS_FILE_SUFFIX;
            Resource resource = this.resourceLoader.getResource(resourcePath);
            return getMetadataReader(resource);
        } catch (FileNotFoundException ex) {
            // Maybe an inner class name using the dot name syntax? Need to use the dollar syntax here...
            // ClassUtils.forName has an equivalent check for resolution into Class references later on.
            int lastDotIndex = className.lastIndexOf('.');
            if (lastDotIndex != -1) {
                String innerClassName = className.substring(0, lastDotIndex) + '$' + className.substring(lastDotIndex + 1);
                String innerClassResourcePath = ResourceLoader.CLASSPATH_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(innerClassName) + ClassUtils.CLASS_FILE_SUFFIX;
                Resource innerClassResource = this.resourceLoader.getResource(innerClassResourcePath);
                if (innerClassResource.exists()) {
                    return getMetadataReader(innerClassResource);
                }
            }
            throw ex;
        }
    }

    @Override
    public MetadataReader getMetadataReader(Resource resource) throws IOException {
        return new SimpleMetadataReader(resource, this.resourceLoader.getClassLoader());
    }

}
