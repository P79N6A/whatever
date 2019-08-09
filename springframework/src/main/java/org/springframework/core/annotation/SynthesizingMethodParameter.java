package org.springframework.core.annotation;

import org.springframework.core.MethodParameter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class SynthesizingMethodParameter extends MethodParameter {

    public SynthesizingMethodParameter(Method method, int parameterIndex) {
        super(method, parameterIndex);
    }

    public SynthesizingMethodParameter(Method method, int parameterIndex, int nestingLevel) {
        super(method, parameterIndex, nestingLevel);
    }

    public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex) {
        super(constructor, parameterIndex);
    }

    public SynthesizingMethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
        super(constructor, parameterIndex, nestingLevel);
    }

    protected SynthesizingMethodParameter(SynthesizingMethodParameter original) {
        super(original);
    }

    @Override
    protected <A extends Annotation> A adaptAnnotation(A annotation) {
        return AnnotationUtils.synthesizeAnnotation(annotation, getAnnotatedElement());
    }

    @Override
    protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
        return AnnotationUtils.synthesizeAnnotationArray(annotations, getAnnotatedElement());
    }

    @Override
    public SynthesizingMethodParameter clone() {
        return new SynthesizingMethodParameter(this);
    }

    public static SynthesizingMethodParameter forExecutable(Executable executable, int parameterIndex) {
        if (executable instanceof Method) {
            return new SynthesizingMethodParameter((Method) executable, parameterIndex);
        } else if (executable instanceof Constructor) {
            return new SynthesizingMethodParameter((Constructor<?>) executable, parameterIndex);
        } else {
            throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
        }
    }

    public static SynthesizingMethodParameter forParameter(Parameter parameter) {
        return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
    }

}
