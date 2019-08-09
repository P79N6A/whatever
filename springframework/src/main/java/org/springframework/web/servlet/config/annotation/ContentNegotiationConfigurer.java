package org.springframework.web.servlet.config.annotation;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.accept.ContentNegotiationManagerFactoryBean;
import org.springframework.web.accept.ContentNegotiationStrategy;

import javax.servlet.ServletContext;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContentNegotiationConfigurer {

    private final ContentNegotiationManagerFactoryBean factory = new ContentNegotiationManagerFactoryBean();

    private final Map<String, MediaType> mediaTypes = new HashMap<>();

    public ContentNegotiationConfigurer(@Nullable ServletContext servletContext) {
        if (servletContext != null) {
            this.factory.setServletContext(servletContext);
        }
    }

    public void strategies(@Nullable List<ContentNegotiationStrategy> strategies) {
        this.factory.setStrategies(strategies);
    }

    public ContentNegotiationConfigurer favorPathExtension(boolean favorPathExtension) {
        this.factory.setFavorPathExtension(favorPathExtension);
        return this;
    }

    public ContentNegotiationConfigurer mediaType(String extension, MediaType mediaType) {
        this.mediaTypes.put(extension, mediaType);
        return this;
    }

    public ContentNegotiationConfigurer mediaTypes(@Nullable Map<String, MediaType> mediaTypes) {
        if (mediaTypes != null) {
            this.mediaTypes.putAll(mediaTypes);
        }
        return this;
    }

    public ContentNegotiationConfigurer replaceMediaTypes(Map<String, MediaType> mediaTypes) {
        this.mediaTypes.clear();
        mediaTypes(mediaTypes);
        return this;
    }

    public ContentNegotiationConfigurer ignoreUnknownPathExtensions(boolean ignore) {
        this.factory.setIgnoreUnknownPathExtensions(ignore);
        return this;
    }

    @Deprecated
    public ContentNegotiationConfigurer useJaf(boolean useJaf) {
        return this.useRegisteredExtensionsOnly(!useJaf);
    }

    public ContentNegotiationConfigurer useRegisteredExtensionsOnly(boolean useRegisteredExtensionsOnly) {
        this.factory.setUseRegisteredExtensionsOnly(useRegisteredExtensionsOnly);
        return this;
    }

    public ContentNegotiationConfigurer favorParameter(boolean favorParameter) {
        this.factory.setFavorParameter(favorParameter);
        return this;
    }

    public ContentNegotiationConfigurer parameterName(String parameterName) {
        this.factory.setParameterName(parameterName);
        return this;
    }

    public ContentNegotiationConfigurer ignoreAcceptHeader(boolean ignoreAcceptHeader) {
        this.factory.setIgnoreAcceptHeader(ignoreAcceptHeader);
        return this;
    }

    public ContentNegotiationConfigurer defaultContentType(MediaType... defaultContentTypes) {
        this.factory.setDefaultContentTypes(Arrays.asList(defaultContentTypes));
        return this;
    }

    public ContentNegotiationConfigurer defaultContentTypeStrategy(ContentNegotiationStrategy defaultStrategy) {
        this.factory.setDefaultContentTypeStrategy(defaultStrategy);
        return this;
    }

    protected ContentNegotiationManager buildContentNegotiationManager() {
        this.factory.addMediaTypes(this.mediaTypes);
        return this.factory.build();
    }

}
