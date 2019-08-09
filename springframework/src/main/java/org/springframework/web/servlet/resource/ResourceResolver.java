package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface ResourceResolver {

    @Nullable
    Resource resolveResource(@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain);

    @Nullable
    String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain);

}
