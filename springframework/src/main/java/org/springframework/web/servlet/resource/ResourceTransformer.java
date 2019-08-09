package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@FunctionalInterface
public interface ResourceTransformer {

    Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain) throws IOException;

}
