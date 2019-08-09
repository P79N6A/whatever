package org.springframework.web.servlet.view.tiles3;

import org.apache.tiles.request.ApplicationResource;
import org.apache.tiles.request.locale.URLApplicationResource;
import org.apache.tiles.request.servlet.ServletApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.support.ServletContextResourcePatternResolver;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class SpringWildcardServletTilesApplicationContext extends ServletApplicationContext {

    private final ResourcePatternResolver resolver;

    public SpringWildcardServletTilesApplicationContext(ServletContext servletContext) {
        super(servletContext);
        this.resolver = new ServletContextResourcePatternResolver(servletContext);
    }

    @Override
    @Nullable
    public ApplicationResource getResource(String localePath) {
        Collection<ApplicationResource> urlSet = getResources(localePath);
        if (!CollectionUtils.isEmpty(urlSet)) {
            return urlSet.iterator().next();
        }
        return null;
    }

    @Override
    @Nullable
    public ApplicationResource getResource(ApplicationResource base, Locale locale) {
        Collection<ApplicationResource> urlSet = getResources(base.getLocalePath(locale));
        if (!CollectionUtils.isEmpty(urlSet)) {
            return urlSet.iterator().next();
        }
        return null;
    }

    @Override
    public Collection<ApplicationResource> getResources(String path) {
        Resource[] resources;
        try {
            resources = this.resolver.getResources(path);
        } catch (IOException ex) {
            ((ServletContext) getContext()).log("Resource retrieval failed for path: " + path, ex);
            return Collections.emptyList();
        }
        if (ObjectUtils.isEmpty(resources)) {
            ((ServletContext) getContext()).log("No resources found for path pattern: " + path);
            return Collections.emptyList();
        }
        Collection<ApplicationResource> resourceList = new ArrayList<>(resources.length);
        for (Resource resource : resources) {
            try {
                URL url = resource.getURL();
                resourceList.add(new URLApplicationResource(url.toExternalForm(), url));
            } catch (IOException ex) {
                // Shouldn't happen with the kind of resources we're using
                throw new IllegalArgumentException("No URL for " + resource, ex);
            }
        }
        return resourceList;
    }

}
