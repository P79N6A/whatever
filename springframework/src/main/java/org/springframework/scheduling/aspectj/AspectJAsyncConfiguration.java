package org.springframework.scheduling.aspectj;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.scheduling.annotation.AbstractAsyncConfiguration;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

@Configuration
public class AspectJAsyncConfiguration extends AbstractAsyncConfiguration {

    // @Bean(name = TaskManagementConfigUtils.ASYNC_EXECUTION_ASPECT_BEAN_NAME)
    // @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    // public AnnotationAsyncExecutionAspect asyncAdvisor() {
    //     AnnotationAsyncExecutionAspect asyncAspect = AnnotationAsyncExecutionAspect.aspectOf();
    //     asyncAspect.configure(this.executor, this.exceptionHandler);
    //     return asyncAspect;
    // }

}
