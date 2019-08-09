package org.springframework.boot.builder;

import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpringApplicationBuilder {

    private final SpringApplication application;

    private ConfigurableApplicationContext context;

    private SpringApplicationBuilder parent;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private final Set<Class<?>> sources = new LinkedHashSet<>();

    private final Map<String, Object> defaultProperties = new LinkedHashMap<>();

    private ConfigurableEnvironment environment;

    private Set<String> additionalProfiles = new LinkedHashSet<>();

    private boolean registerShutdownHookApplied;

    private boolean configuredAsChild = false;

    public SpringApplicationBuilder(Class<?>... sources) {
        this.application = createSpringApplication(sources);
    }

    protected SpringApplication createSpringApplication(Class<?>... sources) {
        return new SpringApplication(sources);
    }

    public ConfigurableApplicationContext context() {
        return this.context;
    }

    public SpringApplication application() {
        return this.application;
    }

    public ConfigurableApplicationContext run(String... args) {
        if (this.running.get()) {
            // If already created we just return the existing context
            return this.context;
        }
        configureAsChildIfNecessary(args);
        if (this.running.compareAndSet(false, true)) {
            synchronized (this.running) {
                // If not already running copy the sources over and then run.
                this.context = build().run(args);
            }
        }
        return this.context;
    }

    private void configureAsChildIfNecessary(String... args) {
        if (this.parent != null && !this.configuredAsChild) {
            this.configuredAsChild = true;
            if (!this.registerShutdownHookApplied) {
                this.application.setRegisterShutdownHook(false);
            }
            initializers(new ParentContextApplicationContextInitializer(this.parent.run(args)));
        }
    }

    public SpringApplication build() {
        return build(new String[0]);
    }

    public SpringApplication build(String... args) {
        configureAsChildIfNecessary(args);
        this.application.addPrimarySources(this.sources);
        return this.application;
    }

    public SpringApplicationBuilder child(Class<?>... sources) {
        SpringApplicationBuilder child = new SpringApplicationBuilder();
        child.sources(sources);
        // Copy environment stuff from parent to child
        child.properties(this.defaultProperties).environment(this.environment).additionalProfiles(this.additionalProfiles);
        child.parent = this;
        // It's not possible if embedded web server are enabled to support web contexts as
        // parents because the servlets cannot be initialized at the right point in
        // lifecycle.
        web(WebApplicationType.NONE);
        // Probably not interested in multiple banners
        bannerMode(Banner.Mode.OFF);
        // Make sure sources get copied over
        this.application.addPrimarySources(this.sources);
        return child;
    }

    public SpringApplicationBuilder parent(Class<?>... sources) {
        if (this.parent == null) {
            this.parent = new SpringApplicationBuilder(sources).web(WebApplicationType.NONE).properties(this.defaultProperties).environment(this.environment);
        } else {
            this.parent.sources(sources);
        }
        return this.parent;
    }

    private SpringApplicationBuilder runAndExtractParent(String... args) {
        if (this.context == null) {
            run(args);
        }
        if (this.parent != null) {
            return this.parent;
        }
        throw new IllegalStateException("No parent defined yet (please use the other overloaded parent methods to set one)");
    }

    public SpringApplicationBuilder parent(ConfigurableApplicationContext parent) {
        this.parent = new SpringApplicationBuilder();
        this.parent.context = parent;
        this.parent.running.set(true);
        return this;
    }

    public SpringApplicationBuilder sibling(Class<?>... sources) {
        return runAndExtractParent().child(sources);
    }

    public SpringApplicationBuilder sibling(Class<?>[] sources, String... args) {
        return runAndExtractParent(args).child(sources);
    }

    public SpringApplicationBuilder contextClass(Class<? extends ConfigurableApplicationContext> cls) {
        this.application.setApplicationContextClass(cls);
        return this;
    }

    public SpringApplicationBuilder sources(Class<?>... sources) {
        this.sources.addAll(new LinkedHashSet<>(Arrays.asList(sources)));
        return this;
    }

    public SpringApplicationBuilder web(WebApplicationType webApplicationType) {
        this.application.setWebApplicationType(webApplicationType);
        return this;
    }

    public SpringApplicationBuilder logStartupInfo(boolean logStartupInfo) {
        this.application.setLogStartupInfo(logStartupInfo);
        return this;
    }

    public SpringApplicationBuilder banner(Banner banner) {
        this.application.setBanner(banner);
        return this;
    }

    public SpringApplicationBuilder bannerMode(Banner.Mode bannerMode) {
        this.application.setBannerMode(bannerMode);
        return this;
    }

    public SpringApplicationBuilder headless(boolean headless) {
        this.application.setHeadless(headless);
        return this;
    }

    public SpringApplicationBuilder registerShutdownHook(boolean registerShutdownHook) {
        this.registerShutdownHookApplied = true;
        this.application.setRegisterShutdownHook(registerShutdownHook);
        return this;
    }

    public SpringApplicationBuilder main(Class<?> mainApplicationClass) {
        this.application.setMainApplicationClass(mainApplicationClass);
        return this;
    }

    public SpringApplicationBuilder addCommandLineProperties(boolean addCommandLineProperties) {
        this.application.setAddCommandLineProperties(addCommandLineProperties);
        return this;
    }

    public SpringApplicationBuilder setAddConversionService(boolean addConversionService) {
        this.application.setAddConversionService(addConversionService);
        return this;
    }

    public SpringApplicationBuilder properties(String... defaultProperties) {
        return properties(getMapFromKeyValuePairs(defaultProperties));
    }

    public SpringApplicationBuilder lazyInitialization(boolean lazyInitialization) {
        this.application.setLazyInitialization(lazyInitialization);
        return this;
    }

    private Map<String, Object> getMapFromKeyValuePairs(String[] properties) {
        Map<String, Object> map = new HashMap<>();
        for (String property : properties) {
            int index = lowestIndexOf(property, ":", "=");
            String key = (index > 0) ? property.substring(0, index) : property;
            String value = (index > 0) ? property.substring(index + 1) : "";
            map.put(key, value);
        }
        return map;
    }

    private int lowestIndexOf(String property, String... candidates) {
        int index = -1;
        for (String candidate : candidates) {
            int candidateIndex = property.indexOf(candidate);
            if (candidateIndex > 0) {
                index = (index != -1) ? Math.min(index, candidateIndex) : candidateIndex;
            }
        }
        return index;
    }

    public SpringApplicationBuilder properties(Properties defaultProperties) {
        return properties(getMapFromProperties(defaultProperties));
    }

    private Map<String, Object> getMapFromProperties(Properties properties) {
        Map<String, Object> map = new HashMap<>();
        for (Object key : Collections.list(properties.propertyNames())) {
            map.put((String) key, properties.get(key));
        }
        return map;
    }

    public SpringApplicationBuilder properties(Map<String, Object> defaults) {
        this.defaultProperties.putAll(defaults);
        this.application.setDefaultProperties(this.defaultProperties);
        if (this.parent != null) {
            this.parent.properties(this.defaultProperties);
            this.parent.environment(this.environment);
        }
        return this;
    }

    public SpringApplicationBuilder profiles(String... profiles) {
        this.additionalProfiles.addAll(Arrays.asList(profiles));
        this.application.setAdditionalProfiles(StringUtils.toStringArray(this.additionalProfiles));
        return this;
    }

    private SpringApplicationBuilder additionalProfiles(Collection<String> additionalProfiles) {
        this.additionalProfiles = new LinkedHashSet<>(additionalProfiles);
        this.application.setAdditionalProfiles(StringUtils.toStringArray(this.additionalProfiles));
        return this;
    }

    public SpringApplicationBuilder beanNameGenerator(BeanNameGenerator beanNameGenerator) {
        this.application.setBeanNameGenerator(beanNameGenerator);
        return this;
    }

    public SpringApplicationBuilder environment(ConfigurableEnvironment environment) {
        this.application.setEnvironment(environment);
        this.environment = environment;
        return this;
    }

    public SpringApplicationBuilder resourceLoader(ResourceLoader resourceLoader) {
        this.application.setResourceLoader(resourceLoader);
        return this;
    }

    public SpringApplicationBuilder initializers(ApplicationContextInitializer<?>... initializers) {
        this.application.addInitializers(initializers);
        return this;
    }

    public SpringApplicationBuilder listeners(ApplicationListener<?>... listeners) {
        this.application.addListeners(listeners);
        return this;
    }

}
