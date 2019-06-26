package org.apache.ibatis.annotations;

import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.mapping.StatementType;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Options {

    enum FlushCachePolicy {

        DEFAULT,

        TRUE,

        FALSE
    }

    boolean useCache() default true;

    FlushCachePolicy flushCache() default FlushCachePolicy.DEFAULT;

    ResultSetType resultSetType() default ResultSetType.DEFAULT;

    StatementType statementType() default StatementType.PREPARED;

    int fetchSize() default -1;

    int timeout() default -1;

    boolean useGeneratedKeys() default false;

    String keyProperty() default "";

    String keyColumn() default "";

    String resultSets() default "";
}
