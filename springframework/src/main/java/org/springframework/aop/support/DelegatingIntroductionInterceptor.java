package org.springframework.aop.support;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.DynamicIntroductionAdvice;
import org.springframework.aop.IntroductionInterceptor;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

@SuppressWarnings("serial")
public class DelegatingIntroductionInterceptor extends IntroductionInfoSupport implements IntroductionInterceptor {

    @Nullable
    private Object delegate;

    public DelegatingIntroductionInterceptor(Object delegate) {
        init(delegate);
    }

    protected DelegatingIntroductionInterceptor() {
        init(this);
    }

    private void init(Object delegate) {
        Assert.notNull(delegate, "Delegate must not be null");
        this.delegate = delegate;
        implementInterfacesOnObject(delegate);
        suppressInterface(IntroductionInterceptor.class);
        suppressInterface(DynamicIntroductionAdvice.class);
    }

    @Override
    @Nullable
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (isMethodOnIntroducedInterface(mi)) {
            Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());
            if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {
                Object proxy = ((ProxyMethodInvocation) mi).getProxy();
                if (mi.getMethod().getReturnType().isInstance(proxy)) {
                    retVal = proxy;
                }
            }
            return retVal;
        }
        return doProceed(mi);
    }

    protected Object doProceed(MethodInvocation mi) throws Throwable {
        return mi.proceed();
    }

}
