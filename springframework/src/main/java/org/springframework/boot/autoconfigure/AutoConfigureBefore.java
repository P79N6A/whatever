package org.springframework.boot.autoconfigure;

import java.lang.annotation.*;

/**
 * Hint for that an {@link EnableAutoConfiguration auto-configuration} should be applied
 * before other specified auto-configuration classes.
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface AutoConfigureBefore {

    /**
     * The auto-configure classes that should have not yet been applied.
     *
     * @return the classes
     */
    Class<?>[] value() default {};

    /**
     * The names of the auto-configure classes that should have not yet been applied.
     *
     * @return the class names
     * @since 1.2.2
     */
    String[] name() default {};

}
