package org.springframework.context.annotation;

public interface AnnotationConfigRegistry {

    void register(Class<?>... annotatedClasses);

    void scan(String... basePackages);

}
