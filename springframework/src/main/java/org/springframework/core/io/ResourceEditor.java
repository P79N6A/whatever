package org.springframework.core.io;

import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

public class ResourceEditor extends PropertyEditorSupport {

    private final ResourceLoader resourceLoader;

    @Nullable
    private PropertyResolver propertyResolver;

    private final boolean ignoreUnresolvablePlaceholders;

    public ResourceEditor() {
        this(new DefaultResourceLoader(), null);
    }

    public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver) {
        this(resourceLoader, propertyResolver, true);
    }

    public ResourceEditor(ResourceLoader resourceLoader, @Nullable PropertyResolver propertyResolver, boolean ignoreUnresolvablePlaceholders) {
        Assert.notNull(resourceLoader, "ResourceLoader must not be null");
        this.resourceLoader = resourceLoader;
        this.propertyResolver = propertyResolver;
        this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
    }

    @Override
    public void setAsText(String text) {
        if (StringUtils.hasText(text)) {
            String locationToUse = resolvePath(text).trim();
            setValue(this.resourceLoader.getResource(locationToUse));
        } else {
            setValue(null);
        }
    }

    protected String resolvePath(String path) {
        if (this.propertyResolver == null) {
            this.propertyResolver = new StandardEnvironment();
        }
        return (this.ignoreUnresolvablePlaceholders ? this.propertyResolver.resolvePlaceholders(path) : this.propertyResolver.resolveRequiredPlaceholders(path));
    }

    @Override
    @Nullable
    public String getAsText() {
        Resource value = (Resource) getValue();
        try {
            // Try to determine URL for resource.
            return (value != null ? value.getURL().toExternalForm() : "");
        } catch (IOException ex) {
            // Couldn't determine resource URL - return null to indicate
            // that there is no appropriate text representation.
            return null;
        }
    }

}
