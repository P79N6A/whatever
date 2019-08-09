package org.springframework.expression.spel.support;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.*;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;

public class ReflectiveMethodResolver implements MethodResolver {

    // Using distance will ensure a more accurate match is discovered,
    // more closely following the Java rules.
    private final boolean useDistance;

    @Nullable
    private Map<Class<?>, MethodFilter> filters;

    public ReflectiveMethodResolver() {
        this.useDistance = true;
    }

    public ReflectiveMethodResolver(boolean useDistance) {
        this.useDistance = useDistance;
    }

    public void registerMethodFilter(Class<?> type, @Nullable MethodFilter filter) {
        if (this.filters == null) {
            this.filters = new HashMap<>();
        }
        if (filter != null) {
            this.filters.put(type, filter);
        } else {
            this.filters.remove(type);
        }
    }

    @Override
    @Nullable
    public MethodExecutor resolve(EvaluationContext context, Object targetObject, String name, List<TypeDescriptor> argumentTypes) throws AccessException {
        try {
            TypeConverter typeConverter = context.getTypeConverter();
            Class<?> type = (targetObject instanceof Class ? (Class<?>) targetObject : targetObject.getClass());
            ArrayList<Method> methods = new ArrayList<>(getMethods(type, targetObject));
            // If a filter is registered for this type, call it
            MethodFilter filter = (this.filters != null ? this.filters.get(type) : null);
            if (filter != null) {
                List<Method> filtered = filter.filter(methods);
                methods = (filtered instanceof ArrayList ? (ArrayList<Method>) filtered : new ArrayList<>(filtered));
            }
            // Sort methods into a sensible order
            if (methods.size() > 1) {
                methods.sort((m1, m2) -> {
                    int m1pl = m1.getParameterCount();
                    int m2pl = m2.getParameterCount();
                    // vararg methods go last
                    if (m1pl == m2pl) {
                        if (!m1.isVarArgs() && m2.isVarArgs()) {
                            return -1;
                        } else if (m1.isVarArgs() && !m2.isVarArgs()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                    return Integer.compare(m1pl, m2pl);
                });
            }
            // Resolve any bridge methods
            for (int i = 0; i < methods.size(); i++) {
                methods.set(i, BridgeMethodResolver.findBridgedMethod(methods.get(i)));
            }
            // Remove duplicate methods (possible due to resolved bridge methods)
            Set<Method> methodsToIterate = new LinkedHashSet<>(methods);
            Method closeMatch = null;
            int closeMatchDistance = Integer.MAX_VALUE;
            Method matchRequiringConversion = null;
            boolean multipleOptions = false;
            for (Method method : methodsToIterate) {
                if (method.getName().equals(name)) {
                    Class<?>[] paramTypes = method.getParameterTypes();
                    List<TypeDescriptor> paramDescriptors = new ArrayList<>(paramTypes.length);
                    for (int i = 0; i < paramTypes.length; i++) {
                        paramDescriptors.add(new TypeDescriptor(new MethodParameter(method, i)));
                    }
                    ReflectionHelper.ArgumentsMatchInfo matchInfo = null;
                    if (method.isVarArgs() && argumentTypes.size() >= (paramTypes.length - 1)) {
                        // *sigh* complicated
                        matchInfo = ReflectionHelper.compareArgumentsVarargs(paramDescriptors, argumentTypes, typeConverter);
                    } else if (paramTypes.length == argumentTypes.size()) {
                        // Name and parameter number match, check the arguments
                        matchInfo = ReflectionHelper.compareArguments(paramDescriptors, argumentTypes, typeConverter);
                    }
                    if (matchInfo != null) {
                        if (matchInfo.isExactMatch()) {
                            return new ReflectiveMethodExecutor(method);
                        } else if (matchInfo.isCloseMatch()) {
                            if (this.useDistance) {
                                int matchDistance = ReflectionHelper.getTypeDifferenceWeight(paramDescriptors, argumentTypes);
                                if (closeMatch == null || matchDistance < closeMatchDistance) {
                                    // This is a better match...
                                    closeMatch = method;
                                    closeMatchDistance = matchDistance;
                                }
                            } else {
                                // Take this as a close match if there isn't one already
                                if (closeMatch == null) {
                                    closeMatch = method;
                                }
                            }
                        } else if (matchInfo.isMatchRequiringConversion()) {
                            if (matchRequiringConversion != null) {
                                multipleOptions = true;
                            }
                            matchRequiringConversion = method;
                        }
                    }
                }
            }
            if (closeMatch != null) {
                return new ReflectiveMethodExecutor(closeMatch);
            } else if (matchRequiringConversion != null) {
                if (multipleOptions) {
                    throw new SpelEvaluationException(SpelMessage.MULTIPLE_POSSIBLE_METHODS, name);
                }
                return new ReflectiveMethodExecutor(matchRequiringConversion);
            } else {
                return null;
            }
        } catch (EvaluationException ex) {
            throw new AccessException("Failed to resolve method", ex);
        }
    }

    private Set<Method> getMethods(Class<?> type, Object targetObject) {
        if (targetObject instanceof Class) {
            Set<Method> result = new LinkedHashSet<>();
            // Add these so that static methods are invocable on the type: e.g. Float.valueOf(..)
            Method[] methods = getMethods(type);
            for (Method method : methods) {
                if (Modifier.isStatic(method.getModifiers())) {
                    result.add(method);
                }
            }
            // Also expose methods from java.lang.Class itself
            Collections.addAll(result, getMethods(Class.class));
            return result;
        } else if (Proxy.isProxyClass(type)) {
            Set<Method> result = new LinkedHashSet<>();
            // Expose interface methods (not proxy-declared overrides) for proper vararg introspection
            for (Class<?> ifc : type.getInterfaces()) {
                Method[] methods = getMethods(ifc);
                for (Method method : methods) {
                    if (isCandidateForInvocation(method, type)) {
                        result.add(method);
                    }
                }
            }
            return result;
        } else {
            Set<Method> result = new LinkedHashSet<>();
            Method[] methods = getMethods(type);
            for (Method method : methods) {
                if (isCandidateForInvocation(method, type)) {
                    result.add(method);
                }
            }
            return result;
        }
    }

    protected Method[] getMethods(Class<?> type) {
        return type.getMethods();
    }

    protected boolean isCandidateForInvocation(Method method, Class<?> targetClass) {
        return true;
    }

}
