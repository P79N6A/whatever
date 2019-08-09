package org.springframework.boot.convert;

import org.springframework.util.unit.DataUnit;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSizeUnit {

    DataUnit value();

}
