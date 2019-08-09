package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface ResourceTransformerChain {

    ResourceResolverChain getResolverChain();

    Resource transform(HttpServletRequest request, Resource resource) throws IOException;

}
