package org.springframework.boot.autoconfigure.validation;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import javax.validation.Validator;
import javax.validation.executable.ExecutableValidator;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ExecutableValidator.class)
@ConditionalOnResource(resources = "classpath:META-INF/services/javax.validation.spi.ValidationProvider")
@Import(PrimaryDefaultValidatorPostProcessor.class)
public class ValidationAutoConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(Validator.class)
    public static LocalValidatorFactoryBean defaultValidator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory();
        factoryBean.setMessageInterpolator(interpolatorFactory.getObject());
        return factoryBean;
    }

    @Bean
    @ConditionalOnMissingBean
    public static MethodValidationPostProcessor methodValidationPostProcessor(Environment environment, @Lazy Validator validator) {
        MethodValidationPostProcessor processor = new MethodValidationPostProcessor();
        boolean proxyTargetClass = environment.getProperty("spring.aop.proxy-target-class", Boolean.class, true);
        processor.setProxyTargetClass(proxyTargetClass);
        processor.setValidator(validator);
        return processor;
    }

}
