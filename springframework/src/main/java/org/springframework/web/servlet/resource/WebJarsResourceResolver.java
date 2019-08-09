package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.webjars.WebJarAssetLocator;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class WebJarsResourceResolver extends AbstractResourceResolver {

    private static final String WEBJARS_LOCATION = "META-INF/resources/webjars/";

    private static final int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();

    private final WebJarAssetLocator webJarAssetLocator;

    public WebJarsResourceResolver() {
        this(new WebJarAssetLocator());
    }

    public WebJarsResourceResolver(WebJarAssetLocator webJarAssetLocator) {
        this.webJarAssetLocator = webJarAssetLocator;
    }

    @Override
    protected Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        Resource resolved = chain.resolveResource(request, requestPath, locations);
        if (resolved == null) {
            String webJarResourcePath = findWebJarResourcePath(requestPath);
            if (webJarResourcePath != null) {
                return chain.resolveResource(request, webJarResourcePath, locations);
            }
        }
        return resolved;
    }

    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        String path = chain.resolveUrlPath(resourceUrlPath, locations);
        if (path == null) {
            String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
            if (webJarResourcePath != null) {
                return chain.resolveUrlPath(webJarResourcePath, locations);
            }
        }
        return path;
    }

    @Nullable
    protected String findWebJarResourcePath(String path) {
        int startOffset = (path.startsWith("/") ? 1 : 0);
        int endOffset = path.indexOf('/', 1);
        if (endOffset != -1) {
            String webjar = path.substring(startOffset, endOffset);
            String partialPath = path.substring(endOffset + 1);
            String webJarPath = this.webJarAssetLocator.getFullPathExact(webjar, partialPath);
            if (webJarPath != null) {
                return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
            }
        }
        return null;
    }

}
