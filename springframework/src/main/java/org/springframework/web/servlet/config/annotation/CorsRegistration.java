package org.springframework.web.servlet.config.annotation;

import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;

public class CorsRegistration {

    private final String pathPattern;

    private final CorsConfiguration config;

    public CorsRegistration(String pathPattern) {
        this.pathPattern = pathPattern;
        // Same implicit default values as the @CrossOrigin annotation + allows simple methods
        this.config = new CorsConfiguration().applyPermitDefaultValues();
    }

    public CorsRegistration allowedOrigins(String... origins) {
        this.config.setAllowedOrigins(Arrays.asList(origins));
        return this;
    }

    public CorsRegistration allowedMethods(String... methods) {
        this.config.setAllowedMethods(Arrays.asList(methods));
        return this;
    }

    public CorsRegistration allowedHeaders(String... headers) {
        this.config.setAllowedHeaders(Arrays.asList(headers));
        return this;
    }

    public CorsRegistration exposedHeaders(String... headers) {
        this.config.setExposedHeaders(Arrays.asList(headers));
        return this;
    }

    public CorsRegistration allowCredentials(boolean allowCredentials) {
        this.config.setAllowCredentials(allowCredentials);
        return this;
    }

    public CorsRegistration maxAge(long maxAge) {
        this.config.setMaxAge(maxAge);
        return this;
    }

    protected String getPathPattern() {
        return this.pathPattern;
    }

    protected CorsConfiguration getCorsConfiguration() {
        return this.config;
    }

}
