package org.springframework.boot.diagnostics.analyzer;

import org.springframework.boot.context.properties.InvalidConfigurationPropertiesException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public class InvalidConfigurationPropertiesFailureAnalyzer extends AbstractFailureAnalyzer<InvalidConfigurationPropertiesException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, InvalidConfigurationPropertiesException cause) {
        String configurationProperties = cause.getConfigurationProperties().getName();
        String component = cause.getComponent().getSimpleName();
        return new FailureAnalysis(getDescription(configurationProperties, component), getAction(configurationProperties, component), cause);
    }

    private String getDescription(String configurationProperties, String component) {
        return configurationProperties + " is annotated with @ConfigurationProperties and @" + component + ". This may cause the @ConfigurationProperties bean to be registered twice.";
    }

    private String getAction(String configurationProperties, String component) {
        return "Remove @" + component + " from " + configurationProperties + " or consider disabling automatic @ConfigurationProperties scanning.";
    }

}
