package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;

public interface VersionStrategy extends VersionPathStrategy {

    String getResourceVersion(Resource resource);

}
