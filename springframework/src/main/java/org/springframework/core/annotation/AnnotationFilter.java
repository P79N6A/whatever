package org.springframework.core.annotation;

import java.lang.annotation.Annotation;

@FunctionalInterface
public interface AnnotationFilter {

    AnnotationFilter PLAIN = packages("java.lang", "org.springframework.lang");

    AnnotationFilter JAVA = packages("java", "javax");

    AnnotationFilter NONE = new AnnotationFilter() {
        @Override
        public boolean matches(String typeName) {
            return false;
        }

        @Override
        public String toString() {
            return "No annotation filtering";
        }
    };

    default boolean matches(Annotation annotation) {
        return matches(annotation.annotationType());
    }

    default boolean matches(Class<?> type) {
        return matches(type.getName());
    }

    boolean matches(String typeName);

    static AnnotationFilter packages(String... packages) {
        return new PackagesAnnotationFilter(packages);
    }

}
