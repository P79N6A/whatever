package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseStatus {

    @AliasFor("code") HttpStatus value() default HttpStatus.INTERNAL_SERVER_ERROR;

    @AliasFor("value") HttpStatus code() default HttpStatus.INTERNAL_SERVER_ERROR;

    String reason() default "";

}
