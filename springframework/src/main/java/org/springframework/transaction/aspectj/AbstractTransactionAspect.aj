package org.springframework.transaction.aspectj;

import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

public abstract aspect AbstractTransactionAspect extends TransactionAspectSupport implements DisposableBean {

    protected AbstractTransactionAspect(TransactionAttributeSource tas) {
        setTransactionAttributeSource(tas);
    }

    @Override
    public void destroy() {
        clearTransactionManagerCache(); // An aspect is basically a singleton
    }

    @SuppressAjWarnings("adviceDidNotMatch")
    Object around(final Object txObject): transactionalMethodExecution(txObject) {
        MethodSignature methodSignature = (MethodSignature) thisJoinPoint.getSignature();
        // Adapt to TransactionAspectSupport's invokeWithinTransaction...
        try {
            return invokeWithinTransaction(methodSignature.getMethod(), txObject.getClass(), new InvocationCallback() {
                public Object proceedWithInvocation() throws Throwable {
                    return proceed(txObject);
                }
            });
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable thr) {
            Rethrower.rethrow(thr);
            throw new IllegalStateException("Should never get here", thr);
        }
    }


    protected abstract pointcut transactionalMethodExecution(Object txObject);



    private static class Rethrower {

        public static void rethrow(final Throwable exception) {
            class CheckedExceptionRethrower<T extends Throwable> {
                @SuppressWarnings("unchecked")
                private void rethrow(Throwable exception) throws T {
                    throw (T) exception;
                }

            }
            new CheckedExceptionRethrower<RuntimeException>().rethrow(exception);
        }

    }

}
