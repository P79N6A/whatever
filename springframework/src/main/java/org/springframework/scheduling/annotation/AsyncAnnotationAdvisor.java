package org.springframework.scheduling.annotation;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@SuppressWarnings("serial")
public class AsyncAnnotationAdvisor extends AbstractPointcutAdvisor implements BeanFactoryAware {

    private Advice advice;

    private Pointcut pointcut;

    public AsyncAnnotationAdvisor() {
        this((Supplier<Executor>) null, (Supplier<AsyncUncaughtExceptionHandler>) null);
    }

    @SuppressWarnings("unchecked")
    public AsyncAnnotationAdvisor(@Nullable Executor executor, @Nullable AsyncUncaughtExceptionHandler exceptionHandler) {
        this(SingletonSupplier.ofNullable(executor), SingletonSupplier.ofNullable(exceptionHandler));
    }

    @SuppressWarnings("unchecked")
    public AsyncAnnotationAdvisor(@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {
        Set<Class<? extends Annotation>> asyncAnnotationTypes = new LinkedHashSet<>(2);
        asyncAnnotationTypes.add(Async.class);
        try {
            asyncAnnotationTypes.add((Class<? extends Annotation>) ClassUtils.forName("javax.ejb.Asynchronous", AsyncAnnotationAdvisor.class.getClassLoader()));
        } catch (ClassNotFoundException ex) {
            // If EJB 3.1 API not present, simply ignore.
        }
        this.advice = buildAdvice(executor, exceptionHandler);
        this.pointcut = buildPointcut(asyncAnnotationTypes);
    }

    public void setAsyncAnnotationType(Class<? extends Annotation> asyncAnnotationType) {
        Assert.notNull(asyncAnnotationType, "'asyncAnnotationType' must not be null");
        Set<Class<? extends Annotation>> asyncAnnotationTypes = new HashSet<>();
        asyncAnnotationTypes.add(asyncAnnotationType);
        this.pointcut = buildPointcut(asyncAnnotationTypes);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (this.advice instanceof BeanFactoryAware) {
            ((BeanFactoryAware) this.advice).setBeanFactory(beanFactory);
        }
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    protected Advice buildAdvice(@Nullable Supplier<Executor> executor, @Nullable Supplier<AsyncUncaughtExceptionHandler> exceptionHandler) {
        AnnotationAsyncExecutionInterceptor interceptor = new AnnotationAsyncExecutionInterceptor(null);
        interceptor.configure(executor, exceptionHandler);
        return interceptor;
    }

    protected Pointcut buildPointcut(Set<Class<? extends Annotation>> asyncAnnotationTypes) {
        ComposablePointcut result = null;
        for (Class<? extends Annotation> asyncAnnotationType : asyncAnnotationTypes) {
            Pointcut cpc = new AnnotationMatchingPointcut(asyncAnnotationType, true);
            Pointcut mpc = new AnnotationMatchingPointcut(null, asyncAnnotationType, true);
            if (result == null) {
                result = new ComposablePointcut(cpc);
            } else {
                result.union(cpc);
            }
            result = result.union(mpc);
        }
        return (result != null ? result : Pointcut.TRUE);
    }

}
