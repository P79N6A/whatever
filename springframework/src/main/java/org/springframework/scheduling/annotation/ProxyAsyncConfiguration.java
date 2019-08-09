package org.springframework.scheduling.annotation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;

@Configuration
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class ProxyAsyncConfiguration extends AbstractAsyncConfiguration {

    @Bean(name = TaskManagementConfigUtils.ASYNC_ANNOTATION_PROCESSOR_BEAN_NAME)
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AsyncAnnotationBeanPostProcessor asyncAdvisor() {
        Assert.notNull(this.enableAsync, "@EnableAsync annotation metadata was not injected");
        AsyncAnnotationBeanPostProcessor bpp = new AsyncAnnotationBeanPostProcessor();
        bpp.configure(this.executor, this.exceptionHandler);
        Class<? extends Annotation> customAsyncAnnotation = this.enableAsync.getClass("annotation");
        if (customAsyncAnnotation != AnnotationUtils.getDefaultValue(EnableAsync.class, "annotation")) {
            bpp.setAsyncAnnotationType(customAsyncAnnotation);
        }
        bpp.setProxyTargetClass(this.enableAsync.getBoolean("proxyTargetClass"));
        bpp.setOrder(this.enableAsync.<Integer>getNumber("order"));
        return bpp;
    }

}
