package org.springframework.context.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(LoadTimeWeavingConfiguration.class)
public @interface EnableLoadTimeWeaving {

    AspectJWeaving aspectjWeaving() default AspectJWeaving.AUTODETECT;

    enum AspectJWeaving {

        /**
         * 开启LoadTimeWeaving
         */
        ENABLED,

        /**
         * 关闭LoadTimeWeaving
         */
        DISABLED,

        /**
         * 自动检测，如果类路径下有META-INF/aop.xml，开启
         */
        AUTODETECT;
    }

}
