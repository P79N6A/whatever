package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CrossOrigin {

    @Deprecated
    String[] DEFAULT_ORIGINS = {"*"};

    @Deprecated
    String[] DEFAULT_ALLOWED_HEADERS = {"*"};

    @Deprecated
    boolean DEFAULT_ALLOW_CREDENTIALS = false;

    @Deprecated
    long DEFAULT_MAX_AGE = 1800;

    @AliasFor("origins") String[] value() default {};

    @AliasFor("value") String[] origins() default {};

    String[] allowedHeaders() default {};

    String[] exposedHeaders() default {};

    RequestMethod[] methods() default {};

    String allowCredentials() default "";

    long maxAge() default -1;

}
