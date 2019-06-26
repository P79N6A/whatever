package org.apache.ibatis.annotations;

import org.apache.ibatis.scripting.LanguageDriver;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Lang {
    Class<? extends LanguageDriver> value();
}
