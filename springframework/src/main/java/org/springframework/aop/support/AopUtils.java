package org.springframework.aop.support;

import org.springframework.aop.*;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AopUtils {

    public static boolean isAopProxy(@Nullable Object object) {
        return (object instanceof SpringProxy && (Proxy.isProxyClass(object.getClass()) || object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)));
    }

    public static boolean isJdkDynamicProxy(@Nullable Object object) {
        return (object instanceof SpringProxy && Proxy.isProxyClass(object.getClass()));
    }

    public static boolean isCglibProxy(@Nullable Object object) {
        return (object instanceof SpringProxy && object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR));
    }

    public static Class<?> getTargetClass(Object candidate) {
        Assert.notNull(candidate, "Candidate object must not be null");
        Class<?> result = null;
        if (candidate instanceof TargetClassAware) {
            result = ((TargetClassAware) candidate).getTargetClass();
        }
        if (result == null) {
            result = (isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
        }
        return result;
    }

    public static Method selectInvocableMethod(Method method, @Nullable Class<?> targetType) {
        if (targetType == null) {
            return method;
        }
        Method methodToUse = MethodIntrospector.selectInvocableMethod(method, targetType);
        if (Modifier.isPrivate(methodToUse.getModifiers()) && !Modifier.isStatic(methodToUse.getModifiers()) && SpringProxy.class.isAssignableFrom(targetType)) {
            throw new IllegalStateException(String.format("Need to invoke method '%s' found on proxy for target class '%s' but cannot " + "be delegated to target bean. Switch its visibility to package or protected.", method.getName(), method.getDeclaringClass().getSimpleName()));
        }
        return methodToUse;
    }

    public static boolean isEqualsMethod(@Nullable Method method) {
        return ReflectionUtils.isEqualsMethod(method);
    }

    public static boolean isHashCodeMethod(@Nullable Method method) {
        return ReflectionUtils.isHashCodeMethod(method);
    }

    public static boolean isToStringMethod(@Nullable Method method) {
        return ReflectionUtils.isToStringMethod(method);
    }

    public static boolean isFinalizeMethod(@Nullable Method method) {
        return (method != null && method.getName().equals("finalize") && method.getParameterCount() == 0);
    }

    public static Method getMostSpecificMethod(Method method, @Nullable Class<?> targetClass) {
        Class<?> specificTargetClass = (targetClass != null ? ClassUtils.getUserClass(targetClass) : null);
        Method resolvedMethod = ClassUtils.getMostSpecificMethod(method, specificTargetClass);
        return BridgeMethodResolver.findBridgedMethod(resolvedMethod);
    }

    public static boolean canApply(Pointcut pc, Class<?> targetClass) {
        return canApply(pc, targetClass, false);
    }

    public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
        Assert.notNull(pc, "Pointcut must not be null");
        // JoinPoint不匹配目标类
        if (!pc.getClassFilter().matches(targetClass)) {
            return false;
        }
        MethodMatcher methodMatcher = pc.getMethodMatcher();
        // 大概就是直接通过不用匹配吧
        if (methodMatcher == MethodMatcher.TRUE) {
            return true;
        }
        IntroductionAwareMethodMatcher introductionAwareMethodMatcher = null;
        if (methodMatcher instanceof IntroductionAwareMethodMatcher) {
            // 转型
            introductionAwareMethodMatcher = (IntroductionAwareMethodMatcher) methodMatcher;
        }
        Set<Class<?>> classes = new LinkedHashSet<>();
        // 没被JDK动态代理
        if (!Proxy.isProxyClass(targetClass)) {
            // 被CGLIB代理就返回父类，否则原样返回
            classes.add(ClassUtils.getUserClass(targetClass));
        }
        // 所有父接口
        classes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetClass));
        // 遍历
        for (Class<?> clazz : classes) {
            // 声明的方法
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(clazz);
            // 遍历
            for (Method method : methods) {
                // 匹配目标类方法
                if (introductionAwareMethodMatcher != null ? introductionAwareMethodMatcher.matches(method, targetClass, hasIntroductions) : methodMatcher.matches(method, targetClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean canApply(Advisor advisor, Class<?> targetClass) {
        return canApply(advisor, targetClass, false);
    }

    public static boolean canApply(Advisor advisor, Class<?> targetClass, boolean hasIntroductions) {
        if (advisor instanceof IntroductionAdvisor) {
            return ((IntroductionAdvisor) advisor).getClassFilter().matches(targetClass);
        }
        // InstantiationModelAwarePointcutAdvisorImpl
        else if (advisor instanceof PointcutAdvisor) {
            PointcutAdvisor pca = (PointcutAdvisor) advisor;
            // 传入AspectJExpressionPointcut
            return canApply(pca.getPointcut(), targetClass, hasIntroductions);
        } else {
            return true;
        }
    }

    public static List<Advisor> findAdvisorsThatCanApply(List<Advisor> candidateAdvisors, Class<?> clazz) {
        // 空
        if (candidateAdvisors.isEmpty()) {
            return candidateAdvisors;
        }
        // 存放合格的Advisor
        List<Advisor> eligibleAdvisors = new ArrayList<>();
        // 遍历
        for (Advisor candidate : candidateAdvisors) {
            /*
             * 引介 && 合格
             * DefaultIntroductionAdvisor：对应IntroductionInfo
             * DeclareParentsAdvisor：对应@DeclareParents
             */
            if (candidate instanceof IntroductionAdvisor && canApply(candidate, clazz)) {
                // 加
                eligibleAdvisors.add(candidate);
            }
        }
        // 有合格的引介Advisor，一般没有
        boolean hasIntroductions = !eligibleAdvisors.isEmpty();
        for (Advisor candidate : candidateAdvisors) {
            // 跳过IntroductionAdvisor，上面验证过了
            if (candidate instanceof IntroductionAdvisor) {
                continue;
            }
            // 验证非IntroductionAdvisor，比如之前AspectJ的AdviceMethod转化的InstantiationModelAwarePointcutAdvisorImpl
            if (canApply(candidate, clazz, hasIntroductions)) {
                // 加
                eligibleAdvisors.add(candidate);
            }
        }
        return eligibleAdvisors;
    }

    @Nullable
    public static Object invokeJoinpointUsingReflection(@Nullable Object target, Method method, Object[] args) throws Throwable {
        try {
            ReflectionUtils.makeAccessible(method);
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        } catch (IllegalArgumentException ex) {
            throw new AopInvocationException("AOP configuration seems to be invalid: tried calling method [" + method + "] on target [" + target + "]", ex);
        } catch (IllegalAccessException ex) {
            throw new AopInvocationException("Could not access method [" + method + "]", ex);
        }
    }

}
