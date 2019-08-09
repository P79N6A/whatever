package org.springframework.web.servlet.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public abstract class AbstractResourceResolver implements ResourceResolver {

    protected final Log logger = LogFactory.getLog(getClass());

    @Override
    @Nullable
    public Resource resolveResource(@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        return resolveResourceInternal(request, requestPath, locations, chain);
    }

    @Override
    @Nullable
    public String resolveUrlPath(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        return resolveUrlPathInternal(resourceUrlPath, locations, chain);
    }

    @Nullable
    protected abstract Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain);

    @Nullable
    protected abstract String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations, ResourceResolverChain chain);

}
