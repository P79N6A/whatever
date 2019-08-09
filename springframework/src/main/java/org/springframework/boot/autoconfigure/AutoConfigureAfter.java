package org.springframework.boot.autoconfigure;

import java.lang.annotation.*;

/**
 * Hint for that an {@link EnableAutoConfiguration auto-configuration} should be applied
 * after other specified auto-configuration classes.
 *
 * @author Phillip Webb
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
public @interface AutoConfigureAfter {

    /**
     * The auto-configure classes that should have already been applied.
     *
     * @return the classes
     */
    Class<?>[] value() default {};

    /**
     * The names of the auto-configure classes that should have already been applied.
     *
     * @return the class names
     * @since 1.2.2
     */
    String[] name() default {};

}
