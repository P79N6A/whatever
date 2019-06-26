package org.apache.ibatis.builder.annotation;

import java.lang.reflect.Method;

public class MethodResolver {
    private final MapperAnnotationBuilder annotationBuilder;
    private final Method method;

    public MethodResolver(MapperAnnotationBuilder annotationBuilder, Method method) {
        this.annotationBuilder = annotationBuilder;
        this.method = method;
    }

    public void resolve() {
        annotationBuilder.parseStatement(method);
    }

}