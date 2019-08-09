package org.springframework.web.servlet.resource;

import org.springframework.lang.Nullable;

public interface VersionPathStrategy {

    @Nullable
    String extractVersion(String requestPath);

    String removeVersion(String requestPath, String version);

    String addVersion(String requestPath, String version);

}
