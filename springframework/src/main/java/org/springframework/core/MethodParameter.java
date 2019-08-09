package org.springframework.core;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MethodParameter {

    private static final Annotation[] EMPTY_ANNOTATION_ARRAY = new Annotation[0];

    private final Executable executable;

    private final int parameterIndex;

    @Nullable
    private volatile Parameter parameter;

    private int nestingLevel = 1;

    @Nullable
    Map<Integer, Integer> typeIndexesPerLevel;

    @Nullable
    private volatile Class<?> containingClass;

    @Nullable
    private volatile Class<?> parameterType;

    @Nullable
    private volatile Type genericParameterType;

    @Nullable
    private volatile Annotation[] parameterAnnotations;

    @Nullable
    private volatile ParameterNameDiscoverer parameterNameDiscoverer;

    @Nullable
    private volatile String parameterName;

    @Nullable
    private volatile MethodParameter nestedMethodParameter;

    public MethodParameter(Method method, int parameterIndex) {
        this(method, parameterIndex, 1);
    }

    public MethodParameter(Method method, int parameterIndex, int nestingLevel) {
        Assert.notNull(method, "Method must not be null");
        this.executable = method;
        this.parameterIndex = validateIndex(method, parameterIndex);
        this.nestingLevel = nestingLevel;
    }

    public MethodParameter(Constructor<?> constructor, int parameterIndex) {
        this(constructor, parameterIndex, 1);
    }

    public MethodParameter(Constructor<?> constructor, int parameterIndex, int nestingLevel) {
        Assert.notNull(constructor, "Constructor must not be null");
        this.executable = constructor;
        this.parameterIndex = validateIndex(constructor, parameterIndex);
        this.nestingLevel = nestingLevel;
    }

    public MethodParameter(MethodParameter original) {
        Assert.notNull(original, "Original must not be null");
        this.executable = original.executable;
        this.parameterIndex = original.parameterIndex;
        this.parameter = original.parameter;
        this.nestingLevel = original.nestingLevel;
        this.typeIndexesPerLevel = original.typeIndexesPerLevel;
        this.containingClass = original.containingClass;
        this.parameterType = original.parameterType;
        this.genericParameterType = original.genericParameterType;
        this.parameterAnnotations = original.parameterAnnotations;
        this.parameterNameDiscoverer = original.parameterNameDiscoverer;
        this.parameterName = original.parameterName;
    }

    @Nullable
    public Method getMethod() {
        return (this.executable instanceof Method ? (Method) this.executable : null);
    }

    @Nullable
    public Constructor<?> getConstructor() {
        return (this.executable instanceof Constructor ? (Constructor<?>) this.executable : null);
    }

    public Class<?> getDeclaringClass() {
        return this.executable.getDeclaringClass();
    }

    public Member getMember() {
        return this.executable;
    }

    public AnnotatedElement getAnnotatedElement() {
        return this.executable;
    }

    public Executable getExecutable() {
        return this.executable;
    }

    public Parameter getParameter() {
        if (this.parameterIndex < 0) {
            throw new IllegalStateException("Cannot retrieve Parameter descriptor for method return type");
        }
        Parameter parameter = this.parameter;
        if (parameter == null) {
            parameter = getExecutable().getParameters()[this.parameterIndex];
            this.parameter = parameter;
        }
        return parameter;
    }

    public int getParameterIndex() {
        return this.parameterIndex;
    }

    public void increaseNestingLevel() {
        this.nestingLevel++;
    }

    public void decreaseNestingLevel() {
        getTypeIndexesPerLevel().remove(this.nestingLevel);
        this.nestingLevel--;
    }

    public int getNestingLevel() {
        return this.nestingLevel;
    }

    public void setTypeIndexForCurrentLevel(int typeIndex) {
        getTypeIndexesPerLevel().put(this.nestingLevel, typeIndex);
    }

    @Nullable
    public Integer getTypeIndexForCurrentLevel() {
        return getTypeIndexForLevel(this.nestingLevel);
    }

    @Nullable
    public Integer getTypeIndexForLevel(int nestingLevel) {
        return getTypeIndexesPerLevel().get(nestingLevel);
    }

    private Map<Integer, Integer> getTypeIndexesPerLevel() {
        if (this.typeIndexesPerLevel == null) {
            this.typeIndexesPerLevel = new HashMap<>(4);
        }
        return this.typeIndexesPerLevel;
    }

    public MethodParameter nested() {
        MethodParameter nestedParam = this.nestedMethodParameter;
        if (nestedParam != null) {
            return nestedParam;
        }
        nestedParam = clone();
        nestedParam.nestingLevel = this.nestingLevel + 1;
        this.nestedMethodParameter = nestedParam;
        return nestedParam;
    }

    public boolean isOptional() {
        return (getParameterType() == Optional.class || hasNullableAnnotation() || (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass()) && KotlinDelegate.isOptional(this)));
    }

    private boolean hasNullableAnnotation() {
        for (Annotation ann : getParameterAnnotations()) {
            if ("Nullable".equals(ann.annotationType().getSimpleName())) {
                return true;
            }
        }
        return false;
    }

    public MethodParameter nestedIfOptional() {
        return (getParameterType() == Optional.class ? nested() : this);
    }

    void setContainingClass(Class<?> containingClass) {
        this.containingClass = containingClass;
    }

    public Class<?> getContainingClass() {
        Class<?> containingClass = this.containingClass;
        return (containingClass != null ? containingClass : getDeclaringClass());
    }

    void setParameterType(@Nullable Class<?> parameterType) {
        this.parameterType = parameterType;
    }

    public Class<?> getParameterType() {
        Class<?> paramType = this.parameterType;
        if (paramType == null) {
            if (this.parameterIndex < 0) {
                Method method = getMethod();
                paramType = (method != null ? (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass()) ? KotlinDelegate.getReturnType(method) : method.getReturnType()) : void.class);
            } else {
                paramType = this.executable.getParameterTypes()[this.parameterIndex];
            }
            this.parameterType = paramType;
        }
        return paramType;
    }

    public Type getGenericParameterType() {
        Type paramType = this.genericParameterType;
        if (paramType == null) {
            if (this.parameterIndex < 0) {
                Method method = getMethod();
                paramType = (method != null ? (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(getContainingClass()) ? KotlinDelegate.getGenericReturnType(method) : method.getGenericReturnType()) : void.class);
            } else {
                Type[] genericParameterTypes = this.executable.getGenericParameterTypes();
                int index = this.parameterIndex;
                if (this.executable instanceof Constructor && ClassUtils.isInnerClass(this.executable.getDeclaringClass()) && genericParameterTypes.length == this.executable.getParameterCount() - 1) {
                    // Bug in javac: type array excludes enclosing instance parameter
                    // for inner classes with at least one generic constructor parameter,
                    // so access it with the actual parameter index lowered by 1
                    index = this.parameterIndex - 1;
                }
                paramType = (index >= 0 && index < genericParameterTypes.length ? genericParameterTypes[index] : getParameterType());
            }
            this.genericParameterType = paramType;
        }
        return paramType;
    }

    public Class<?> getNestedParameterType() {
        if (this.nestingLevel > 1) {
            Type type = getGenericParameterType();
            for (int i = 2; i <= this.nestingLevel; i++) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    Integer index = getTypeIndexForLevel(i);
                    type = args[index != null ? index : args.length - 1];
                }
                // TODO: Object.class if unresolvable
            }
            if (type instanceof Class) {
                return (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                Type arg = ((ParameterizedType) type).getRawType();
                if (arg instanceof Class) {
                    return (Class<?>) arg;
                }
            }
            return Object.class;
        } else {
            return getParameterType();
        }
    }

    public Type getNestedGenericParameterType() {
        if (this.nestingLevel > 1) {
            Type type = getGenericParameterType();
            for (int i = 2; i <= this.nestingLevel; i++) {
                if (type instanceof ParameterizedType) {
                    Type[] args = ((ParameterizedType) type).getActualTypeArguments();
                    Integer index = getTypeIndexForLevel(i);
                    type = args[index != null ? index : args.length - 1];
                }
            }
            return type;
        } else {
            return getGenericParameterType();
        }
    }

    public Annotation[] getMethodAnnotations() {
        return adaptAnnotationArray(getAnnotatedElement().getAnnotations());
    }

    @Nullable
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
        A annotation = getAnnotatedElement().getAnnotation(annotationType);
        return (annotation != null ? adaptAnnotation(annotation) : null);
    }

    public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
        return getAnnotatedElement().isAnnotationPresent(annotationType);
    }

    public Annotation[] getParameterAnnotations() {
        Annotation[] paramAnns = this.parameterAnnotations;
        if (paramAnns == null) {
            Annotation[][] annotationArray = this.executable.getParameterAnnotations();
            int index = this.parameterIndex;
            if (this.executable instanceof Constructor && ClassUtils.isInnerClass(this.executable.getDeclaringClass()) && annotationArray.length == this.executable.getParameterCount() - 1) {
                // Bug in javac in JDK <9: annotation array excludes enclosing instance parameter
                // for inner classes, so access it with the actual parameter index lowered by 1
                index = this.parameterIndex - 1;
            }
            paramAnns = (index >= 0 && index < annotationArray.length ? adaptAnnotationArray(annotationArray[index]) : EMPTY_ANNOTATION_ARRAY);
            this.parameterAnnotations = paramAnns;
        }
        return paramAnns;
    }

    public boolean hasParameterAnnotations() {
        return (getParameterAnnotations().length != 0);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <A extends Annotation> A getParameterAnnotation(Class<A> annotationType) {
        Annotation[] anns = getParameterAnnotations();
        for (Annotation ann : anns) {
            if (annotationType.isInstance(ann)) {
                return (A) ann;
            }
        }
        return null;
    }

    public <A extends Annotation> boolean hasParameterAnnotation(Class<A> annotationType) {
        return (getParameterAnnotation(annotationType) != null);
    }

    public void initParameterNameDiscovery(@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    @Nullable
    public String getParameterName() {
        if (this.parameterIndex < 0) {
            return null;
        }
        ParameterNameDiscoverer discoverer = this.parameterNameDiscoverer;
        if (discoverer != null) {
            String[] parameterNames = null;
            if (this.executable instanceof Method) {
                parameterNames = discoverer.getParameterNames((Method) this.executable);
            } else if (this.executable instanceof Constructor) {
                parameterNames = discoverer.getParameterNames((Constructor<?>) this.executable);
            }
            if (parameterNames != null) {
                this.parameterName = parameterNames[this.parameterIndex];
            }
            this.parameterNameDiscoverer = null;
        }
        return this.parameterName;
    }

    protected <A extends Annotation> A adaptAnnotation(A annotation) {
        return annotation;
    }

    protected Annotation[] adaptAnnotationArray(Annotation[] annotations) {
        return annotations;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MethodParameter)) {
            return false;
        }
        MethodParameter otherParam = (MethodParameter) other;
        return (this.parameterIndex == otherParam.parameterIndex && getExecutable().equals(otherParam.getExecutable()));
    }

    @Override
    public int hashCode() {
        return (getExecutable().hashCode() * 31 + this.parameterIndex);
    }

    @Override
    public String toString() {
        Method method = getMethod();
        return (method != null ? "method '" + method.getName() + "'" : "constructor") + " parameter " + this.parameterIndex;
    }

    @Override
    public MethodParameter clone() {
        return new MethodParameter(this);
    }

    @Deprecated
    public static MethodParameter forMethodOrConstructor(Object methodOrConstructor, int parameterIndex) {
        if (!(methodOrConstructor instanceof Executable)) {
            throw new IllegalArgumentException("Given object [" + methodOrConstructor + "] is neither a Method nor a Constructor");
        }
        return forExecutable((Executable) methodOrConstructor, parameterIndex);
    }

    public static MethodParameter forExecutable(Executable executable, int parameterIndex) {
        if (executable instanceof Method) {
            return new MethodParameter((Method) executable, parameterIndex);
        } else if (executable instanceof Constructor) {
            return new MethodParameter((Constructor<?>) executable, parameterIndex);
        } else {
            throw new IllegalArgumentException("Not a Method/Constructor: " + executable);
        }
    }

    public static MethodParameter forParameter(Parameter parameter) {
        return forExecutable(parameter.getDeclaringExecutable(), findParameterIndex(parameter));
    }

    protected static int findParameterIndex(Parameter parameter) {
        Executable executable = parameter.getDeclaringExecutable();
        Parameter[] allParams = executable.getParameters();
        // Try first with identity checks for greater performance.
        for (int i = 0; i < allParams.length; i++) {
            if (parameter == allParams[i]) {
                return i;
            }
        }
        // Potentially try again with object equality checks in order to avoid race
        // conditions while invoking java.lang.reflect.Executable.getParameters().
        for (int i = 0; i < allParams.length; i++) {
            if (parameter.equals(allParams[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("Given parameter [" + parameter + "] does not match any parameter in the declaring executable");
    }

    private static int validateIndex(Executable executable, int parameterIndex) {
        int count = executable.getParameterCount();
        Assert.isTrue(parameterIndex >= -1 && parameterIndex < count, () -> "Parameter index needs to be between -1 and " + (count - 1));
        return parameterIndex;
    }

    private static class KotlinDelegate {

        public static boolean isOptional(MethodParameter param) {
            Method method = param.getMethod();
            Constructor<?> ctor = param.getConstructor();
            int index = param.getParameterIndex();
            if (method != null && index == -1) {
                KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
                return (function != null && function.getReturnType().isMarkedNullable());
            } else {
                KFunction<?> function = null;
                Predicate<KParameter> predicate = null;
                if (method != null) {
                    function = ReflectJvmMapping.getKotlinFunction(method);
                    predicate = p -> KParameter.Kind.VALUE.equals(p.getKind());
                } else if (ctor != null) {
                    function = ReflectJvmMapping.getKotlinFunction(ctor);
                    predicate = p -> KParameter.Kind.VALUE.equals(p.getKind()) || KParameter.Kind.INSTANCE.equals(p.getKind());
                }
                if (function != null) {
                    List<KParameter> parameters = function.getParameters();
                    KParameter parameter = parameters.stream().filter(predicate).collect(Collectors.toList()).get(index);
                    return (parameter.getType().isMarkedNullable() || parameter.isOptional());
                }
            }
            return false;
        }

        static private Type getGenericReturnType(Method method) {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
            if (function != null && function.isSuspend()) {
                return ReflectJvmMapping.getJavaType(function.getReturnType());
            }
            return method.getGenericReturnType();
        }

        static private Class<?> getReturnType(Method method) {
            KFunction<?> function = ReflectJvmMapping.getKotlinFunction(method);
            if (function != null && function.isSuspend()) {
                Type paramType = ReflectJvmMapping.getJavaType(function.getReturnType());
                Class<?> paramClass = ResolvableType.forType(paramType).resolve();
                Assert.notNull(paramClass, "Type " + paramType + "can't be resolved to a class");
                return paramClass;
            }
            return method.getReturnType();
        }

    }

}
