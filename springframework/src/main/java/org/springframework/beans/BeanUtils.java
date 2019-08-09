package org.springframework.beans;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.*;

public abstract class BeanUtils {

    private static final Log logger = LogFactory.getLog(BeanUtils.class);

    private static final Set<Class<?>> unknownEditorTypes = Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

    private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES;

    static {
        Map<Class<?>, Object> values = new HashMap<>();
        values.put(boolean.class, false);
        values.put(byte.class, (byte) 0);
        values.put(short.class, (short) 0);
        values.put(int.class, 0);
        values.put(long.class, (long) 0);
        DEFAULT_TYPE_VALUES = Collections.unmodifiableMap(values);
    }

    @Deprecated
    public static <T> T instantiate(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            return clazz.newInstance();
        } catch (InstantiationException ex) {
            throw new BeanInstantiationException(clazz, "Is it an abstract class?", ex);
        } catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(clazz, "Is the constructor accessible?", ex);
        }
    }

    public static <T> T instantiateClass(Class<T> clazz) throws BeanInstantiationException {
        Assert.notNull(clazz, "Class must not be null");
        if (clazz.isInterface()) {
            throw new BeanInstantiationException(clazz, "Specified class is an interface");
        }
        try {
            return instantiateClass(clazz.getDeclaredConstructor());
        } catch (NoSuchMethodException ex) {
            Constructor<T> ctor = findPrimaryConstructor(clazz);
            if (ctor != null) {
                return instantiateClass(ctor);
            }
            throw new BeanInstantiationException(clazz, "No default constructor found", ex);
        } catch (LinkageError err) {
            throw new BeanInstantiationException(clazz, "Unresolvable class definition", err);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T instantiateClass(Class<?> clazz, Class<T> assignableTo) throws BeanInstantiationException {
        Assert.isAssignable(assignableTo, clazz);
        return (T) instantiateClass(clazz);
    }

    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws BeanInstantiationException {
        Assert.notNull(ctor, "Constructor must not be null");
        try {
            ReflectionUtils.makeAccessible(ctor);
            if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(ctor.getDeclaringClass())) {
                return KotlinDelegate.instantiateClass(ctor, args);
            } else {
                Class<?>[] parameterTypes = ctor.getParameterTypes();
                Assert.isTrue(args.length <= parameterTypes.length, "Can't specify more arguments than constructor parameters");
                Object[] argsWithDefaultValues = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] == null) {
                        Class<?> parameterType = parameterTypes[i];
                        argsWithDefaultValues[i] = (parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null);
                    } else {
                        argsWithDefaultValues[i] = args[i];
                    }
                }
                return ctor.newInstance(argsWithDefaultValues);
            }
        } catch (InstantiationException ex) {
            throw new BeanInstantiationException(ctor, "Is it an abstract class?", ex);
        } catch (IllegalAccessException ex) {
            throw new BeanInstantiationException(ctor, "Is the constructor accessible?", ex);
        } catch (IllegalArgumentException ex) {
            throw new BeanInstantiationException(ctor, "Illegal arguments for constructor", ex);
        } catch (InvocationTargetException ex) {
            throw new BeanInstantiationException(ctor, "Constructor threw exception", ex.getTargetException());
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        if (KotlinDetector.isKotlinReflectPresent() && KotlinDetector.isKotlinType(clazz)) {
            Constructor<T> kotlinPrimaryConstructor = KotlinDelegate.findPrimaryConstructor(clazz);
            if (kotlinPrimaryConstructor != null) {
                return kotlinPrimaryConstructor;
            }
        }
        return null;
    }

    @Nullable
    public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            return findDeclaredMethod(clazz, methodName, paramTypes);
        }
    }

    @Nullable
    public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException ex) {
            if (clazz.getSuperclass() != null) {
                return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
            }
            return null;
        }
    }

    @Nullable
    public static Method findMethodWithMinimalParameters(Class<?> clazz, String methodName) throws IllegalArgumentException {
        Method targetMethod = findMethodWithMinimalParameters(clazz.getMethods(), methodName);
        if (targetMethod == null) {
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz, methodName);
        }
        return targetMethod;
    }

    @Nullable
    public static Method findDeclaredMethodWithMinimalParameters(Class<?> clazz, String methodName) throws IllegalArgumentException {
        Method targetMethod = findMethodWithMinimalParameters(clazz.getDeclaredMethods(), methodName);
        if (targetMethod == null && clazz.getSuperclass() != null) {
            targetMethod = findDeclaredMethodWithMinimalParameters(clazz.getSuperclass(), methodName);
        }
        return targetMethod;
    }

    @Nullable
    public static Method findMethodWithMinimalParameters(Method[] methods, String methodName) throws IllegalArgumentException {
        Method targetMethod = null;
        int numMethodsFoundWithCurrentMinimumArgs = 0;
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                int numParams = method.getParameterCount();
                if (targetMethod == null || numParams < targetMethod.getParameterCount()) {
                    targetMethod = method;
                    numMethodsFoundWithCurrentMinimumArgs = 1;
                } else if (!method.isBridge() && targetMethod.getParameterCount() == numParams) {
                    if (targetMethod.isBridge()) {
                        // Prefer regular method over bridge...
                        targetMethod = method;
                    } else {
                        // Additional candidate with same length
                        numMethodsFoundWithCurrentMinimumArgs++;
                    }
                }
            }
        }
        if (numMethodsFoundWithCurrentMinimumArgs > 1) {
            throw new IllegalArgumentException("Cannot resolve method '" + methodName + "' to a unique method. Attempted to resolve to overloaded method with " + "the least number of parameters but there were " + numMethodsFoundWithCurrentMinimumArgs + " candidates.");
        }
        return targetMethod;
    }

    @Nullable
    public static Method resolveSignature(String signature, Class<?> clazz) {
        Assert.hasText(signature, "'signature' must not be empty");
        Assert.notNull(clazz, "Class must not be null");
        int startParen = signature.indexOf('(');
        int endParen = signature.indexOf(')');
        if (startParen > -1 && endParen == -1) {
            throw new IllegalArgumentException("Invalid method signature '" + signature + "': expected closing ')' for args list");
        } else if (startParen == -1 && endParen > -1) {
            throw new IllegalArgumentException("Invalid method signature '" + signature + "': expected opening '(' for args list");
        } else if (startParen == -1) {
            return findMethodWithMinimalParameters(clazz, signature);
        } else {
            String methodName = signature.substring(0, startParen);
            String[] parameterTypeNames = StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
            Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
            for (int i = 0; i < parameterTypeNames.length; i++) {
                String parameterTypeName = parameterTypeNames[i].trim();
                try {
                    parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
                } catch (Throwable ex) {
                    throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" + parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
                }
            }
            return findMethod(clazz, methodName, parameterTypes);
        }
    }

    public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
        CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
        return cr.getPropertyDescriptors();
    }

    @Nullable
    public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) throws BeansException {
        CachedIntrospectionResults cr = CachedIntrospectionResults.forClass(clazz);
        return cr.getPropertyDescriptor(propertyName);
    }

    @Nullable
    public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
        return findPropertyForMethod(method, method.getDeclaringClass());
    }

    @Nullable
    public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
        Assert.notNull(method, "Method must not be null");
        PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
        for (PropertyDescriptor pd : pds) {
            if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
                return pd;
            }
        }
        return null;
    }

    @Nullable
    public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType) {
        if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
            return null;
        }
        ClassLoader cl = targetType.getClassLoader();
        if (cl == null) {
            try {
                cl = ClassLoader.getSystemClassLoader();
                if (cl == null) {
                    return null;
                }
            } catch (Throwable ex) {
                // e.g. AccessControlException on Google App Engine
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not access system ClassLoader: " + ex);
                }
                return null;
            }
        }
        String editorName = targetType.getName() + "Editor";
        try {
            Class<?> editorClass = cl.loadClass(editorName);
            if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
                if (logger.isInfoEnabled()) {
                    logger.info("Editor class [" + editorName + "] does not implement [java.beans.PropertyEditor] interface");
                }
                unknownEditorTypes.add(targetType);
                return null;
            }
            return (PropertyEditor) instantiateClass(editorClass);
        } catch (ClassNotFoundException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("No property editor [" + editorName + "] found for type " + targetType.getName() + " according to 'Editor' suffix convention");
            }
            unknownEditorTypes.add(targetType);
            return null;
        }
    }

    public static Class<?> findPropertyType(String propertyName, @Nullable Class<?>... beanClasses) {
        if (beanClasses != null) {
            for (Class<?> beanClass : beanClasses) {
                PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
                if (pd != null) {
                    return pd.getPropertyType();
                }
            }
        }
        return Object.class;
    }

    public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
        if (pd instanceof GenericTypeAwarePropertyDescriptor) {
            return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
        } else {
            Method writeMethod = pd.getWriteMethod();
            Assert.state(writeMethod != null, "No write method available");
            return new MethodParameter(writeMethod, 0);
        }
    }

    public static boolean isSimpleProperty(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        return isSimpleValueType(clazz) || (clazz.isArray() && isSimpleValueType(clazz.getComponentType()));
    }

    public static boolean isSimpleValueType(Class<?> clazz) {
        return (ClassUtils.isPrimitiveOrWrapper(clazz) || Enum.class.isAssignableFrom(clazz) || CharSequence.class.isAssignableFrom(clazz) || Number.class.isAssignableFrom(clazz) || Date.class.isAssignableFrom(clazz) || Temporal.class.isAssignableFrom(clazz) || URI.class == clazz || URL.class == clazz || Locale.class == clazz || Class.class == clazz);
    }

    public static void copyProperties(Object source, Object target) throws BeansException {
        copyProperties(source, target, null, (String[]) null);
    }

    public static void copyProperties(Object source, Object target, Class<?> editable) throws BeansException {
        copyProperties(source, target, editable, (String[]) null);
    }

    public static void copyProperties(Object source, Object target, String... ignoreProperties) throws BeansException {
        copyProperties(source, target, null, ignoreProperties);
    }

    private static void copyProperties(Object source, Object target, @Nullable Class<?> editable, @Nullable String... ignoreProperties) throws BeansException {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(target, "Target must not be null");
        Class<?> actualEditable = target.getClass();
        if (editable != null) {
            if (!editable.isInstance(target)) {
                throw new IllegalArgumentException("Target class [" + target.getClass().getName() + "] not assignable to Editable class [" + editable.getName() + "]");
            }
            actualEditable = editable;
        }
        PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
        List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);
        for (PropertyDescriptor targetPd : targetPds) {
            Method writeMethod = targetPd.getWriteMethod();
            if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {
                PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
                if (sourcePd != null) {
                    Method readMethod = sourcePd.getReadMethod();
                    if (readMethod != null && ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())) {
                        try {
                            if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
                                readMethod.setAccessible(true);
                            }
                            Object value = readMethod.invoke(source);
                            if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
                                writeMethod.setAccessible(true);
                            }
                            writeMethod.invoke(target, value);
                        } catch (Throwable ex) {
                            throw new FatalBeanException("Could not copy property '" + targetPd.getName() + "' from source to target", ex);
                        }
                    }
                }
            }
        }
    }

    private static class KotlinDelegate {

        @Nullable
        public static <T> Constructor<T> findPrimaryConstructor(Class<T> clazz) {
            try {
                KFunction<T> primaryCtor = KClasses.getPrimaryConstructor(JvmClassMappingKt.getKotlinClass(clazz));
                if (primaryCtor == null) {
                    return null;
                }
                Constructor<T> constructor = ReflectJvmMapping.getJavaConstructor(primaryCtor);
                if (constructor == null) {
                    throw new IllegalStateException("Failed to find Java constructor for Kotlin primary constructor: " + clazz.getName());
                }
                return constructor;
            } catch (UnsupportedOperationException ex) {
                return null;
            }
        }

        public static <T> T instantiateClass(Constructor<T> ctor, Object... args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
            KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(ctor);
            if (kotlinConstructor == null) {
                return ctor.newInstance(args);
            }
            List<KParameter> parameters = kotlinConstructor.getParameters();
            Map<KParameter, Object> argParameters = new HashMap<>(parameters.size());
            Assert.isTrue(args.length <= parameters.size(), "Number of provided arguments should be less of equals than number of constructor parameters");
            for (int i = 0; i < args.length; i++) {
                if (!(parameters.get(i).isOptional() && args[i] == null)) {
                    argParameters.put(parameters.get(i), args[i]);
                }
            }
            return kotlinConstructor.callBy(argParameters);
        }

    }

}
