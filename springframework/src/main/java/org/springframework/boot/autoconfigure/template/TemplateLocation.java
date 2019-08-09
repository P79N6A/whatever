package org.springframework.boot.autoconfigure.template;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.IOException;

public class TemplateLocation {

    private final String path;

    public TemplateLocation(String path) {
        Assert.notNull(path, "Path must not be null");
        this.path = path;
    }

    public boolean exists(ResourcePatternResolver resolver) {
        Assert.notNull(resolver, "Resolver must not be null");
        if (resolver.getResource(this.path).exists()) {
            return true;
        }
        try {
            return anyExists(resolver);
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean anyExists(ResourcePatternResolver resolver) throws IOException {
        String searchPath = this.path;
        if (searchPath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
            searchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + searchPath.substring(ResourceLoader.CLASSPATH_URL_PREFIX.length());
        }
        if (searchPath.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
            Resource[] resources = resolver.getResources(searchPath);
            for (Resource resource : resources) {
                if (resource.exists()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return this.path;
    }

}
