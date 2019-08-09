package org.springframework.boot.autoconfigure.template;

import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

@FunctionalInterface
public interface TemplateAvailabilityProvider {

    boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader, ResourceLoader resourceLoader);

}
