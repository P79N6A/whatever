package org.springframework.boot.convert;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DurationFormat {

    DurationStyle value();

}
