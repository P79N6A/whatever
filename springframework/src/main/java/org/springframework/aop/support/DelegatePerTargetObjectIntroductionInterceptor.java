package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.util.Map;
import java.util.WeakHashMap;

@SuppressWarnings("serial")
public class DelegatePerTargetObjectIntroductionInterceptor extends IntroductionInfoSupport implements IntroductionInterceptor {

    private final Map<Object, Object> delegateMap = new WeakHashMap<>();

    private Class<?> defaultImplType;

    private Class<?> interfaceType;

    public DelegatePerTargetObjectIntroductionInterceptor(Class<?> defaultImplType, Class<?> interfaceType) {
        this.defaultImplType = defaultImplType;
        this.interfaceType = interfaceType;
        Object delegate = createNewDelegate();
        implementInterfacesOnObject(delegate);
        suppressInterface(IntroductionInterceptor.class);
        suppressInterface(DynamicIntroductionAdvice.class);
    }

    @Override
    @Nullable
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (isMethodOnIntroducedInterface(mi)) {
            Object delegate = getIntroductionDelegateFor(mi.getThis());
            Object retVal = AopUtils.invokeJoinpointUsingReflection(delegate, mi.getMethod(), mi.getArguments());
            if (retVal == delegate && mi instanceof ProxyMethodInvocation) {
                retVal = ((ProxyMethodInvocation) mi).getProxy();
            }
            return retVal;
        }
        return doProceed(mi);
    }

    protected Object doProceed(MethodInvocation mi) throws Throwable {
        return mi.proceed();
    }

    private Object getIntroductionDelegateFor(Object targetObject) {
        synchronized (this.delegateMap) {
            if (this.delegateMap.containsKey(targetObject)) {
                return this.delegateMap.get(targetObject);
            } else {
                Object delegate = createNewDelegate();
                this.delegateMap.put(targetObject, delegate);
                return delegate;
            }
        }
    }

    private Object createNewDelegate() {
        try {
            return ReflectionUtils.accessibleConstructor(this.defaultImplType).newInstance();
        } catch (Throwable ex) {
            throw new IllegalArgumentException("Cannot create default implementation for '" + this.interfaceType.getName() + "' mixin (" + this.defaultImplType.getName() + "): " + ex);
        }
    }

}
