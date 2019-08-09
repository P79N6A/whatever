package org.springframework.lang;

import java.lang.annotation.*;

@Deprecated
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
@Documented
public @interface UsesSunHttpServer {
}
