package org.springframework.boot.web.server;

import org.springframework.beans.factory.annotation.Value;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Value("${local.server.port}")
public @interface LocalServerPort {

}
