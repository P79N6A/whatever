package org.springframework.boot.env;

import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@FunctionalInterface
public interface EnvironmentPostProcessor {

    void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application);

}
