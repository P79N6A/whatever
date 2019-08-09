package org.springframework.boot.convert;

import java.lang.annotation.*;
import java.time.temporal.ChronoUnit;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DurationUnit {

    ChronoUnit value();

}
