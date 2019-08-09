package org.springframework.boot.autoconfigure.condition;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnCloudPlatformCondition.class)
public @interface ConditionalOnCloudPlatform {

    CloudPlatform value();

}
