package org.springframework.context.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.lang.reflect.Method;
import java.util.Map;

abstract class BeanAnnotationHelper {

    private static final Map<Method, String> beanNameCache = new ConcurrentReferenceHashMap<>();

    private static final Map<Method, Boolean> scopedProxyCache = new ConcurrentReferenceHashMap<>();

    public static boolean isBeanAnnotated(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, Bean.class);
    }

    public static String determineBeanNameFor(Method beanMethod) {
        String beanName = beanNameCache.get(beanMethod);
        if (beanName == null) {
            // By default, the bean name is the name of the @Bean-annotated method
            beanName = beanMethod.getName();
            // Check to see if the user has explicitly set a custom bean name...
            AnnotationAttributes bean = AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Bean.class, false, false);
            if (bean != null) {
                String[] names = bean.getStringArray("name");
                if (names.length > 0) {
                    beanName = names[0];
                }
            }
            beanNameCache.put(beanMethod, beanName);
        }
        return beanName;
    }

    public static boolean isScopedProxy(Method beanMethod) {
        Boolean scopedProxy = scopedProxyCache.get(beanMethod);
        if (scopedProxy == null) {
            AnnotationAttributes scope = AnnotatedElementUtils.findMergedAnnotationAttributes(beanMethod, Scope.class, false, false);
            scopedProxy = (scope != null && scope.getEnum("proxyMode") != ScopedProxyMode.NO);
            scopedProxyCache.put(beanMethod, scopedProxy);
        }
        return scopedProxy;
    }

}
