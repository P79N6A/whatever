package org.springframework.boot.diagnostics.analyzer;

import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import java.io.PrintWriter;
import java.io.StringWriter;

class BeanDefinitionOverrideFailureAnalyzer extends AbstractFailureAnalyzer<BeanDefinitionOverrideException> {

    private static final String ACTION = "Consider renaming one of the beans or enabling " + "overriding by setting spring.main.allow-bean-definition-overriding=true";

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, BeanDefinitionOverrideException cause) {
        return new FailureAnalysis(getDescription(cause), ACTION, cause);
    }

    private String getDescription(BeanDefinitionOverrideException ex) {
        StringWriter description = new StringWriter();
        PrintWriter printer = new PrintWriter(description);
        printer.printf("The bean '%s', defined in %s, could not be registered. A bean with that " + "name has already been defined in %s and overriding is disabled.", ex.getBeanName(), ex.getBeanDefinition().getResourceDescription(), ex.getExistingDefinition().getResourceDescription());
        return description.toString();
    }

}
