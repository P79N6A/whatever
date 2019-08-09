package org.springframework.boot.autoconfigure.web;

import org.springframework.beans.factory.annotation.Value;

public class ErrorProperties {

    @Value("${error.path:/error}")
    private String path = "/error";

    private boolean includeException;

    private IncludeStacktrace includeStacktrace = IncludeStacktrace.NEVER;

    private final Whitelabel whitelabel = new Whitelabel();

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isIncludeException() {
        return this.includeException;
    }

    public void setIncludeException(boolean includeException) {
        this.includeException = includeException;
    }

    public IncludeStacktrace getIncludeStacktrace() {
        return this.includeStacktrace;
    }

    public void setIncludeStacktrace(IncludeStacktrace includeStacktrace) {
        this.includeStacktrace = includeStacktrace;
    }

    public Whitelabel getWhitelabel() {
        return this.whitelabel;
    }

    public enum IncludeStacktrace {

        NEVER,

        ALWAYS,

        ON_TRACE_PARAM

    }

    public static class Whitelabel {

        private boolean enabled = true;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }

}
