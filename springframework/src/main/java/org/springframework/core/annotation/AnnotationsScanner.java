package org.springframework.core.annotation;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiPredicate;

abstract class AnnotationsScanner {

    private static final Annotation[] NO_ANNOTATIONS = {};

    private static final Method[] NO_METHODS = {};

    private static final Map<AnnotatedElement, Annotation[]> declaredAnnotationCache = new ConcurrentReferenceHashMap<>(256);

    private static final Map<Class<?>, Method[]> baseTypeMethodsCache = new ConcurrentReferenceHashMap<>(256);

    private AnnotationsScanner() {
    }

    @Nullable
    static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor) {
        return scan(context, source, searchStrategy, processor, null);
    }

    @Nullable
    static <C, R> R scan(C context, AnnotatedElement source, SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        R result = process(context, source, searchStrategy, processor, classFilter);
        return processor.finish(result);
    }

    @Nullable
    private static <C, R> R process(C context, AnnotatedElement source, SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        if (source instanceof Class) {
            return processClass(context, (Class<?>) source, searchStrategy, processor, classFilter);
        }
        if (source instanceof Method) {
            return processMethod(context, (Method) source, searchStrategy, processor, classFilter);
        }
        return processElement(context, source, processor, classFilter);
    }

    @Nullable
    private static <C, R> R processClass(C context, Class<?> source, SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        switch (searchStrategy) {
            case DIRECT:
                return processElement(context, source, processor, classFilter);
            case INHERITED_ANNOTATIONS:
                return processClassInheritedAnnotations(context, source, processor, classFilter);
            case SUPERCLASS:
                return processClassHierarchy(context, new int[]{0}, source, processor, classFilter, false);
            case EXHAUSTIVE:
                return processClassHierarchy(context, new int[]{0}, source, processor, classFilter, true);
        }
        throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
    }

    @Nullable
    private static <C, R> R processClassInheritedAnnotations(C context, Class<?> source, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        if (isWithoutHierarchy(source)) {
            return processElement(context, source, processor, classFilter);
        }
        Annotation[] relevant = null;
        int remaining = Integer.MAX_VALUE;
        int aggregateIndex = 0;
        Class<?> root = source;
        while (source != null && source != Object.class && remaining > 0 && !hasPlainJavaAnnotationsOnly(source)) {
            R result = processor.doWithAggregate(context, aggregateIndex);
            if (result != null) {
                return result;
            }
            if (isFiltered(source, context, classFilter)) {
                continue;
            }
            Annotation[] declaredAnnotations = getDeclaredAnnotations(context, source, classFilter, true);
            if (relevant == null && declaredAnnotations.length > 0) {
                relevant = root.getAnnotations();
                remaining = relevant.length;
            }
            for (int i = 0; i < declaredAnnotations.length; i++) {
                if (declaredAnnotations[i] != null) {
                    boolean isRelevant = false;
                    for (int relevantIndex = 0; relevantIndex < relevant.length; relevantIndex++) {
                        if (relevant[relevantIndex] != null && declaredAnnotations[i].annotationType() == relevant[relevantIndex].annotationType()) {
                            isRelevant = true;
                            relevant[relevantIndex] = null;
                            remaining--;
                            break;
                        }
                    }
                    if (!isRelevant) {
                        declaredAnnotations[i] = null;
                    }
                }
            }
            result = processor.doWithAnnotations(context, aggregateIndex, source, declaredAnnotations);
            if (result != null) {
                return result;
            }
            source = source.getSuperclass();
            aggregateIndex++;
        }
        return null;
    }

    @Nullable
    private static <C, R> R processClassHierarchy(C context, int[] aggregateIndex, Class<?> source, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter, boolean includeInterfaces) {
        R result = processor.doWithAggregate(context, aggregateIndex[0]);
        if (result != null) {
            return result;
        }
        if (hasPlainJavaAnnotationsOnly(source)) {
            return null;
        }
        Annotation[] annotations = getDeclaredAnnotations(context, source, classFilter, false);
        result = processor.doWithAnnotations(context, aggregateIndex[0], source, annotations);
        if (result != null) {
            return result;
        }
        aggregateIndex[0]++;
        if (includeInterfaces) {
            for (Class<?> interfaceType : source.getInterfaces()) {
                R interfacesResult = processClassHierarchy(context, aggregateIndex, interfaceType, processor, classFilter, true);
                if (interfacesResult != null) {
                    return interfacesResult;
                }
            }
        }
        Class<?> superclass = source.getSuperclass();
        if (superclass != Object.class && superclass != null) {
            R superclassResult = processClassHierarchy(context, aggregateIndex, superclass, processor, classFilter, includeInterfaces);
            if (superclassResult != null) {
                return superclassResult;
            }
        }
        return null;
    }

    @Nullable
    private static <C, R> R processMethod(C context, Method source, SearchStrategy searchStrategy, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        switch (searchStrategy) {
            case DIRECT:
            case INHERITED_ANNOTATIONS:
                return processMethodInheritedAnnotations(context, source, processor, classFilter);
            case SUPERCLASS:
                return processMethodHierarchy(context, new int[]{0}, source.getDeclaringClass(), processor, classFilter, source, false);
            case EXHAUSTIVE:
                return processMethodHierarchy(context, new int[]{0}, source.getDeclaringClass(), processor, classFilter, source, true);
        }
        throw new IllegalStateException("Unsupported search strategy " + searchStrategy);
    }

    @Nullable
    private static <C, R> R processMethodInheritedAnnotations(C context, Method source, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        R result = processor.doWithAggregate(context, 0);
        return (result != null ? result : processMethodAnnotations(context, 0, source, processor, classFilter));
    }

    @Nullable
    private static <C, R> R processMethodHierarchy(C context, int[] aggregateIndex, Class<?> sourceClass, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter, Method rootMethod, boolean includeInterfaces) {
        R result = processor.doWithAggregate(context, aggregateIndex[0]);
        if (result != null) {
            return result;
        }
        if (hasPlainJavaAnnotationsOnly(sourceClass)) {
            return null;
        }
        boolean calledProcessor = false;
        if (sourceClass == rootMethod.getDeclaringClass()) {
            result = processMethodAnnotations(context, aggregateIndex[0], rootMethod, processor, classFilter);
            calledProcessor = true;
            if (result != null) {
                return result;
            }
        } else {
            for (Method candidateMethod : getBaseTypeMethods(context, sourceClass, classFilter)) {
                if (candidateMethod != null && isOverride(rootMethod, candidateMethod)) {
                    result = processMethodAnnotations(context, aggregateIndex[0], candidateMethod, processor, classFilter);
                    calledProcessor = true;
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        if (Modifier.isPrivate(rootMethod.getModifiers())) {
            return null;
        }
        if (calledProcessor) {
            aggregateIndex[0]++;
        }
        if (includeInterfaces) {
            for (Class<?> interfaceType : sourceClass.getInterfaces()) {
                R interfacesResult = processMethodHierarchy(context, aggregateIndex, interfaceType, processor, classFilter, rootMethod, true);
                if (interfacesResult != null) {
                    return interfacesResult;
                }
            }
        }
        Class<?> superclass = sourceClass.getSuperclass();
        if (superclass != Object.class && superclass != null) {
            R superclassResult = processMethodHierarchy(context, aggregateIndex, superclass, processor, classFilter, rootMethod, includeInterfaces);
            if (superclassResult != null) {
                return superclassResult;
            }
        }
        return null;
    }

    private static <C> Method[] getBaseTypeMethods(C context, Class<?> baseType, @Nullable BiPredicate<C, Class<?>> classFilter) {
        if (baseType == Object.class || hasPlainJavaAnnotationsOnly(baseType) || isFiltered(baseType, context, classFilter)) {
            return NO_METHODS;
        }
        Method[] methods = baseTypeMethodsCache.get(baseType);
        if (methods == null) {
            boolean isInterface = baseType.isInterface();
            methods = isInterface ? baseType.getMethods() : ReflectionUtils.getDeclaredMethods(baseType);
            int cleared = 0;
            for (int i = 0; i < methods.length; i++) {
                if ((!isInterface && Modifier.isPrivate(methods[i].getModifiers())) || hasPlainJavaAnnotationsOnly(methods[i]) || getDeclaredAnnotations(methods[i], false).length == 0) {
                    methods[i] = null;
                    cleared++;
                }
            }
            if (cleared == methods.length) {
                methods = NO_METHODS;
            }
            baseTypeMethodsCache.put(baseType, methods);
        }
        return methods;
    }

    private static boolean isOverride(Method rootMethod, Method candidateMethod) {
        return (!Modifier.isPrivate(candidateMethod.getModifiers()) && candidateMethod.getName().equals(rootMethod.getName()) && hasSameParameterTypes(rootMethod, candidateMethod));
    }

    private static boolean hasSameParameterTypes(Method rootMethod, Method candidateMethod) {
        if (candidateMethod.getParameterCount() != rootMethod.getParameterCount()) {
            return false;
        }
        Class<?>[] rootParameterTypes = rootMethod.getParameterTypes();
        Class<?>[] candidateParameterTypes = candidateMethod.getParameterTypes();
        if (Arrays.equals(candidateParameterTypes, rootParameterTypes)) {
            return true;
        }
        return hasSameGenericTypeParameters(rootMethod, candidateMethod, rootParameterTypes);
    }

    private static boolean hasSameGenericTypeParameters(Method rootMethod, Method candidateMethod, Class<?>[] rootParameterTypes) {
        Class<?> sourceDeclaringClass = rootMethod.getDeclaringClass();
        Class<?> candidateDeclaringClass = candidateMethod.getDeclaringClass();
        if (!candidateDeclaringClass.isAssignableFrom(sourceDeclaringClass)) {
            return false;
        }
        for (int i = 0; i < rootParameterTypes.length; i++) {
            Class<?> resolvedParameterType = ResolvableType.forMethodParameter(candidateMethod, i, sourceDeclaringClass).resolve();
            if (rootParameterTypes[i] != resolvedParameterType) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static <C, R> R processMethodAnnotations(C context, int aggregateIndex, Method source, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        Annotation[] annotations = getDeclaredAnnotations(context, source, classFilter, false);
        R result = processor.doWithAnnotations(context, aggregateIndex, source, annotations);
        if (result != null) {
            return result;
        }
        Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(source);
        if (bridgedMethod != source) {
            Annotation[] bridgedAnnotations = getDeclaredAnnotations(context, bridgedMethod, classFilter, true);
            for (int i = 0; i < bridgedAnnotations.length; i++) {
                if (ObjectUtils.containsElement(annotations, bridgedAnnotations[i])) {
                    bridgedAnnotations[i] = null;
                }
            }
            return processor.doWithAnnotations(context, aggregateIndex, source, bridgedAnnotations);
        }
        return null;
    }

    @Nullable
    private static <C, R> R processElement(C context, AnnotatedElement source, AnnotationsProcessor<C, R> processor, @Nullable BiPredicate<C, Class<?>> classFilter) {
        R result = processor.doWithAggregate(context, 0);
        return (result != null ? result : processor.doWithAnnotations(context, 0, source, getDeclaredAnnotations(context, source, classFilter, false)));
    }

    private static <C, R> Annotation[] getDeclaredAnnotations(C context, AnnotatedElement source, @Nullable BiPredicate<C, Class<?>> classFilter, boolean copy) {
        if (source instanceof Class && isFiltered((Class<?>) source, context, classFilter)) {
            return NO_ANNOTATIONS;
        }
        if (source instanceof Method && isFiltered(((Method) source).getDeclaringClass(), context, classFilter)) {
            return NO_ANNOTATIONS;
        }
        return getDeclaredAnnotations(source, copy);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static <A extends Annotation> A getDeclaredAnnotation(AnnotatedElement source, Class<A> annotationType) {
        Annotation[] annotations = getDeclaredAnnotations(source, false);
        for (Annotation annotation : annotations) {
            if (annotation != null && annotationType == annotation.annotationType()) {
                return (A) annotation;
            }
        }
        return null;
    }

    static Annotation[] getDeclaredAnnotations(AnnotatedElement source, boolean defensive) {
        boolean cached = false;
        Annotation[] annotations = declaredAnnotationCache.get(source);
        if (annotations != null) {
            cached = true;
        } else {
            annotations = source.getDeclaredAnnotations();
            if (annotations.length != 0) {
                boolean allIgnored = true;
                for (int i = 0; i < annotations.length; i++) {
                    Annotation annotation = annotations[i];
                    if (isIgnorable(annotation.annotationType()) || !AttributeMethods.forAnnotationType(annotation.annotationType()).isValid(annotation)) {
                        annotations[i] = null;
                    } else {
                        allIgnored = false;
                    }
                }
                annotations = (allIgnored ? NO_ANNOTATIONS : annotations);
                if (source instanceof Class || source instanceof Member) {
                    declaredAnnotationCache.put(source, annotations);
                    cached = true;
                }
            }
        }
        if (!defensive || annotations.length == 0 || !cached) {
            return annotations;
        }
        return annotations.clone();
    }

    private static <C> boolean isFiltered(Class<?> sourceClass, C context, @Nullable BiPredicate<C, Class<?>> classFilter) {
        return (classFilter != null && classFilter.test(context, sourceClass));
    }

    private static boolean isIgnorable(Class<?> annotationType) {
        return AnnotationFilter.PLAIN.matches(annotationType);
    }

    static boolean isKnownEmpty(AnnotatedElement source, SearchStrategy searchStrategy) {
        if (hasPlainJavaAnnotationsOnly(source)) {
            return true;
        }
        if (searchStrategy == SearchStrategy.DIRECT || isWithoutHierarchy(source)) {
            if (source instanceof Method && ((Method) source).isBridge()) {
                return false;
            }
            return getDeclaredAnnotations(source, false).length == 0;
        }
        return false;
    }

    static boolean hasPlainJavaAnnotationsOnly(@Nullable Object annotatedElement) {
        if (annotatedElement instanceof Class) {
            return hasPlainJavaAnnotationsOnly((Class<?>) annotatedElement);
        } else if (annotatedElement instanceof Member) {
            return hasPlainJavaAnnotationsOnly(((Member) annotatedElement).getDeclaringClass());
        } else {
            return false;
        }
    }

    static boolean hasPlainJavaAnnotationsOnly(Class<?> type) {
        return (type.getName().startsWith("java.") || type == Ordered.class);
    }

    private static boolean isWithoutHierarchy(AnnotatedElement source) {
        if (source == Object.class) {
            return true;
        }
        if (source instanceof Class) {
            Class<?> sourceClass = (Class<?>) source;
            return (sourceClass.getSuperclass() == Object.class && sourceClass.getInterfaces().length == 0);
        }
        if (source instanceof Method) {
            Method sourceMethod = (Method) source;
            return (Modifier.isPrivate(sourceMethod.getModifiers()) || isWithoutHierarchy(sourceMethod.getDeclaringClass()));
        }
        return true;
    }

    static void clearCache() {
        declaredAnnotationCache.clear();
        baseTypeMethodsCache.clear();
    }

}
