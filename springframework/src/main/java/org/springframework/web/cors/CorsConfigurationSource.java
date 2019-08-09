package org.springframework.web.cors;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

public interface CorsConfigurationSource {

    @Nullable
    CorsConfiguration getCorsConfiguration(HttpServletRequest request);

}
