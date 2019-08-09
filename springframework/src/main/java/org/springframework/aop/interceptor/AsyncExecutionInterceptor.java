package org.springframework.aop.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class AsyncExecutionInterceptor extends AsyncExecutionAspectSupport implements MethodInterceptor, Ordered {

    public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor) {
        super(defaultExecutor);
    }

    public AsyncExecutionInterceptor(@Nullable Executor defaultExecutor, AsyncUncaughtExceptionHandler exceptionHandler) {
        super(defaultExecutor, exceptionHandler);
    }

    @Override
    @Nullable
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
        Method specificMethod = ClassUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
        final Method userDeclaredMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);
        AsyncTaskExecutor executor = determineAsyncExecutor(userDeclaredMethod);
        if (executor == null) {
            throw new IllegalStateException("No executor specified and no default executor set on AsyncExecutionInterceptor either");
        }
        Callable<Object> task = () -> {
            try {
                Object result = invocation.proceed();
                if (result instanceof Future) {
                    return ((Future<?>) result).get();
                }
            } catch (ExecutionException ex) {
                handleError(ex.getCause(), userDeclaredMethod, invocation.getArguments());
            } catch (Throwable ex) {
                handleError(ex, userDeclaredMethod, invocation.getArguments());
            }
            return null;
        };
        return doSubmit(task, executor, invocation.getMethod().getReturnType());
    }

    @Override
    @Nullable
    protected String getExecutorQualifier(Method method) {
        return null;
    }

    @Override
    @Nullable
    protected Executor getDefaultExecutor(@Nullable BeanFactory beanFactory) {
        Executor defaultExecutor = super.getDefaultExecutor(beanFactory);
        return (defaultExecutor != null ? defaultExecutor : new SimpleAsyncTaskExecutor());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
