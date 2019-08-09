package org.springframework.boot.context.properties;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({
        ConfigurationPropertiesBeanRegistrar.class, // 把@EnableConfigurationProperties导入的类注册到BeanFactory
        ConfigurationPropertiesBindingPostProcessorRegistrar.class // 注册处理注入的BeanPostProcessor
})
public @interface EnableConfigurationProperties {

    Class<?>[] value() default {};

}
