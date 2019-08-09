package org.springframework.boot.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

public class ContextIdApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {

    private int order = Ordered.LOWEST_PRECEDENCE - 10;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ContextId contextId = getContextId(applicationContext);
        applicationContext.setId(contextId.getId());
        applicationContext.getBeanFactory().registerSingleton(ContextId.class.getName(), contextId);
    }

    private ContextId getContextId(ConfigurableApplicationContext applicationContext) {
        ApplicationContext parent = applicationContext.getParent();
        if (parent != null && parent.containsBean(ContextId.class.getName())) {
            return parent.getBean(ContextId.class).createChildId();
        }
        return new ContextId(getApplicationId(applicationContext.getEnvironment()));
    }

    private String getApplicationId(ConfigurableEnvironment environment) {
        String name = environment.getProperty("spring.application.name");
        return StringUtils.hasText(name) ? name : "application";
    }

    class ContextId {

        private final AtomicLong children = new AtomicLong(0);

        private final String id;

        ContextId(String id) {
            this.id = id;
        }

        ContextId createChildId() {
            return new ContextId(this.id + "-" + this.children.incrementAndGet());
        }

        String getId() {
            return this.id;
        }

    }

}
