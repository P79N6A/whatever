package org.springframework.web.servlet.support;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public abstract class AbstractAnnotationConfigDispatcherServletInitializer extends AbstractDispatcherServletInitializer {

    @Override
    @Nullable
    protected WebApplicationContext createRootApplicationContext() {
        Class<?>[] configClasses = getRootConfigClasses();
        if (!ObjectUtils.isEmpty(configClasses)) {
            AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
            context.register(configClasses);
            return context;
        } else {
            return null;
        }
    }

    @Override
    protected WebApplicationContext createServletApplicationContext() {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        Class<?>[] configClasses = getServletConfigClasses();
        if (!ObjectUtils.isEmpty(configClasses)) {
            context.register(configClasses);
        }
        return context;
    }

    @Nullable
    protected abstract Class<?>[] getRootConfigClasses();

    @Nullable
    protected abstract Class<?>[] getServletConfigClasses();

}
