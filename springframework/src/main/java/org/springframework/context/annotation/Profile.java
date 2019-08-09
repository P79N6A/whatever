package org.springframework.context.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(ProfileCondition.class)
public @interface Profile {

    /**
     * The set of profiles for which the annotated component should be registered.
     */
    String[] value();

}
