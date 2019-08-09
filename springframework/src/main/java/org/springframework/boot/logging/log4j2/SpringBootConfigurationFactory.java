package org.springframework.boot.logging.log4j2;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.*;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.springframework.boot.logging.LoggingSystem;

@Plugin(name = "SpringBootConfigurationFactory", category = ConfigurationFactory.CATEGORY)
@Order(0)
public class SpringBootConfigurationFactory extends ConfigurationFactory {

    private static final String[] TYPES = {".springboot"};

    @Override
    protected String[] getSupportedTypes() {
        return TYPES;
    }

    @Override
    public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
        if (source != null && source != ConfigurationSource.NULL_SOURCE && LoggingSystem.get(loggerContext.getClass().getClassLoader()) != null) {
            return new SpringBootConfiguration();
        }
        return null;
    }

    private static final class SpringBootConfiguration extends DefaultConfiguration {

        private SpringBootConfiguration() {
            this.isShutdownHookEnabled = false;
        }

    }

}
