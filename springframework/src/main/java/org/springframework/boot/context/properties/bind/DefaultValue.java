package org.springframework.boot.context.properties.bind;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@Documented
public @interface DefaultValue {

    String[] value();

}
