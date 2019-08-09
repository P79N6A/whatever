package org.springframework.boot.context.config;

import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

public class AnsiOutputApplicationListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Binder.get(environment).bind("spring.output.ansi.enabled", AnsiOutput.Enabled.class).ifBound(AnsiOutput::setEnabled);
        AnsiOutput.setConsoleAvailable(environment.getProperty("spring.output.ansi.console-available", Boolean.class));
    }

    @Override
    public int getOrder() {
        // Apply after ConfigFileApplicationListener has called EnvironmentPostProcessors
        return ConfigFileApplicationListener.DEFAULT_ORDER + 1;
    }

}
