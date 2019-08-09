package org.springframework.transaction.interceptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.MethodClassKey;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFallbackTransactionAttributeSource implements TransactionAttributeSource {

    @SuppressWarnings("serial")
    private static final TransactionAttribute NULL_TRANSACTION_ATTRIBUTE = new DefaultTransactionAttribute() {
        @Override
        public String toString() {
            return "null";
        }
    };

    protected final Log logger = LogFactory.getLog(getClass());

    private final Map<Object, TransactionAttribute> attributeCache = new ConcurrentHashMap<>(1024);

    @Override
    @Nullable
    public TransactionAttribute getTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        if (method.getDeclaringClass() == Object.class) {
            return null;
        }
        Object cacheKey = getCacheKey(method, targetClass);
        // 先查询缓存
        TransactionAttribute cached = this.attributeCache.get(cacheKey);
        if (cached != null) {
            // 标志没有事务
            if (cached == NULL_TRANSACTION_ATTRIBUTE) {
                return null;
            } else {
                return cached;
            }
        } else {
            // 获取方法上的事务配置属性
            TransactionAttribute txAttr = computeTransactionAttribute(method, targetClass);
            // Put it in the cache.
            if (txAttr == null) {
                // 标志没有事务
                this.attributeCache.put(cacheKey, NULL_TRANSACTION_ATTRIBUTE);
            } else {
                String methodIdentification = ClassUtils.getQualifiedMethodName(method, targetClass);
                if (txAttr instanceof DefaultTransactionAttribute) {
                    ((DefaultTransactionAttribute) txAttr).setDescriptor(methodIdentification);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Adding transactional method '" + methodIdentification + "' with attribute: " + txAttr);
                }
                this.attributeCache.put(cacheKey, txAttr);
            }
            return txAttr;
        }
    }

    protected Object getCacheKey(Method method, @Nullable Class<?> targetClass) {
        return new MethodClassKey(method, targetClass);
    }

    @Nullable
    protected TransactionAttribute computeTransactionAttribute(Method method, @Nullable Class<?> targetClass) {
        // 非public方法不允许事务
        if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
            return null;
        }
        // 从CGLIB代理对象（子类）上的一个方法，找到真实对象（父类）上对应的方法，或者原本返回
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetClass);
        /*
         * 先找方法上的事务注解
         * 通过TransactionAnnotationParser解析不同的事务注解
         * @javax.transaction.Transactional或@org.springframework.transaction.annotation.Transactional或别的
         * 包装信息为TransactionAttribute类型返回
         */
        TransactionAttribute txAttr = findTransactionAttribute(specificMethod);
        if (txAttr != null) {
            return txAttr;
        }
        // 方法上没有，就从类上找
        txAttr = findTransactionAttribute(specificMethod.getDeclaringClass());
        if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
            return txAttr;
        }
        // 说明子类重写了方法
        if (specificMethod != method) {
            // 找父类的方法
            txAttr = findTransactionAttribute(method);
            if (txAttr != null) {
                return txAttr;
            }
            // 从最初的类上找（声明方法的类）
            txAttr = findTransactionAttribute(method.getDeclaringClass());
            if (txAttr != null && ClassUtils.isUserLevelMethod(method)) {
                return txAttr;
            }
        }
        return null;
    }

    @Nullable
    protected abstract TransactionAttribute findTransactionAttribute(Class<?> clazz);

    @Nullable
    protected abstract TransactionAttribute findTransactionAttribute(Method method);

    protected boolean allowPublicMethodsOnly() {
        return false;
    }

}
