package org.springframework.core.io.support;

import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Map;

public class ResourcePropertySource extends PropertiesPropertySource {

    @Nullable
    private final String resourceName;

    public ResourcePropertySource(String name, EncodedResource resource) throws IOException {
        super(name, PropertiesLoaderUtils.loadProperties(resource));
        this.resourceName = getNameForResource(resource.getResource());
    }

    public ResourcePropertySource(EncodedResource resource) throws IOException {
        super(getNameForResource(resource.getResource()), PropertiesLoaderUtils.loadProperties(resource));
        this.resourceName = null;
    }

    public ResourcePropertySource(String name, Resource resource) throws IOException {
        super(name, PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
        this.resourceName = getNameForResource(resource);
    }

    public ResourcePropertySource(Resource resource) throws IOException {
        super(getNameForResource(resource), PropertiesLoaderUtils.loadProperties(new EncodedResource(resource)));
        this.resourceName = null;
    }

    public ResourcePropertySource(String name, String location, ClassLoader classLoader) throws IOException {
        this(name, new DefaultResourceLoader(classLoader).getResource(location));
    }

    public ResourcePropertySource(String location, ClassLoader classLoader) throws IOException {
        this(new DefaultResourceLoader(classLoader).getResource(location));
    }

    public ResourcePropertySource(String name, String location) throws IOException {
        this(name, new DefaultResourceLoader().getResource(location));
    }

    public ResourcePropertySource(String location) throws IOException {
        this(new DefaultResourceLoader().getResource(location));
    }

    private ResourcePropertySource(String name, @Nullable String resourceName, Map<String, Object> source) {
        super(name, source);
        this.resourceName = resourceName;
    }

    public ResourcePropertySource withName(String name) {
        if (this.name.equals(name)) {
            return this;
        }
        // Store the original resource name if necessary...
        if (this.resourceName != null) {
            if (this.resourceName.equals(name)) {
                return new ResourcePropertySource(this.resourceName, null, this.source);
            } else {
                return new ResourcePropertySource(name, this.resourceName, this.source);
            }
        } else {
            // Current name is resource name -> preserve it in the extra field...
            return new ResourcePropertySource(name, this.name, this.source);
        }
    }

    public ResourcePropertySource withResourceName() {
        if (this.resourceName == null) {
            return this;
        }
        return new ResourcePropertySource(this.resourceName, null, this.source);
    }

    private static String getNameForResource(Resource resource) {
        String name = resource.getDescription();
        if (!StringUtils.hasText(name)) {
            name = resource.getClass().getSimpleName() + "@" + System.identityHashCode(resource);
        }
        return name;
    }

}
