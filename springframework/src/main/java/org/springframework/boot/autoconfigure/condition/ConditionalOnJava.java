package org.springframework.boot.autoconfigure.condition;

import org.springframework.boot.system.JavaVersion;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnJavaCondition.class)
public @interface ConditionalOnJava {

    Range range() default Range.EQUAL_OR_NEWER;

    JavaVersion value();

    enum Range {

        EQUAL_OR_NEWER,

        OLDER_THAN

    }

}
