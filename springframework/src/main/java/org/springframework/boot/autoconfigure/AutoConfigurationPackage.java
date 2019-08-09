package org.springframework.boot.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Indicates that the package containing the annotated class should be registered with
 * {@link AutoConfigurationPackages}.
 *
 * @author Phillip Webb
 * @see AutoConfigurationPackages
 * @since 1.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(AutoConfigurationPackages.Registrar.class)
public @interface AutoConfigurationPackage {

}
