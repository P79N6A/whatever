package org.springframework.web.method;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Predicate;

public final class HandlerTypePredicate implements Predicate<Class<?>> {

    private final Set<String> basePackages;

    private final List<Class<?>> assignableTypes;

    private final List<Class<? extends Annotation>> annotations;

    private HandlerTypePredicate(Set<String> basePackages, List<Class<?>> assignableTypes, List<Class<? extends Annotation>> annotations) {
        this.basePackages = Collections.unmodifiableSet(basePackages);
        this.assignableTypes = Collections.unmodifiableList(assignableTypes);
        this.annotations = Collections.unmodifiableList(annotations);
    }

    @Override
    public boolean test(Class<?> controllerType) {
        if (!hasSelectors()) {
            return true;
        } else if (controllerType != null) {
            for (String basePackage : this.basePackages) {
                if (controllerType.getName().startsWith(basePackage)) {
                    return true;
                }
            }
            for (Class<?> clazz : this.assignableTypes) {
                if (ClassUtils.isAssignable(clazz, controllerType)) {
                    return true;
                }
            }
            for (Class<? extends Annotation> annotationClass : this.annotations) {
                if (AnnotationUtils.findAnnotation(controllerType, annotationClass) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSelectors() {
        return (!this.basePackages.isEmpty() || !this.assignableTypes.isEmpty() || !this.annotations.isEmpty());
    }
    // Static factory methods

    public static HandlerTypePredicate forAnyHandlerType() {
        return new HandlerTypePredicate(Collections.emptySet(), Collections.emptyList(), Collections.emptyList());
    }

    public static HandlerTypePredicate forBasePackage(String... packages) {
        return new Builder().basePackage(packages).build();
    }

    public static HandlerTypePredicate forBasePackageClass(Class<?>... packageClasses) {
        return new Builder().basePackageClass(packageClasses).build();
    }

    public static HandlerTypePredicate forAssignableType(Class<?>... types) {
        return new Builder().assignableType(types).build();
    }

    @SafeVarargs
    public static HandlerTypePredicate forAnnotation(Class<? extends Annotation>... annotations) {
        return new Builder().annotation(annotations).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Set<String> basePackages = new LinkedHashSet<>();

        private final List<Class<?>> assignableTypes = new ArrayList<>();

        private final List<Class<? extends Annotation>> annotations = new ArrayList<>();

        public Builder basePackage(String... packages) {
            Arrays.stream(packages).filter(StringUtils::hasText).forEach(this::addBasePackage);
            return this;
        }

        public Builder basePackageClass(Class<?>... packageClasses) {
            Arrays.stream(packageClasses).forEach(clazz -> addBasePackage(ClassUtils.getPackageName(clazz)));
            return this;
        }

        private void addBasePackage(String basePackage) {
            this.basePackages.add(basePackage.endsWith(".") ? basePackage : basePackage + ".");
        }

        public Builder assignableType(Class<?>... types) {
            this.assignableTypes.addAll(Arrays.asList(types));
            return this;
        }

        @SuppressWarnings("unchecked")
        public final Builder annotation(Class<? extends Annotation>... annotations) {
            this.annotations.addAll(Arrays.asList(annotations));
            return this;
        }

        public HandlerTypePredicate build() {
            return new HandlerTypePredicate(this.basePackages, this.assignableTypes, this.annotations);
        }

    }

}
