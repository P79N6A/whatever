// package org.springframework.scheduling.aspectj;
//
// import org.aspectj.lang.annotation.SuppressAjWarnings;
// import org.aspectj.lang.reflect.MethodSignature;
// import org.springframework.aop.interceptor.AsyncExecutionAspectSupport;
// import org.springframework.core.task.AsyncTaskExecutor;
//
// import java.util.concurrent.Callable;
// import java.util.concurrent.Future;
//
// public abstract aspect AbstractAsyncExecutionAspect extends AsyncExecutionAspectSupport {
//
//     public AbstractAsyncExecutionAspect() {
//         super(null);
//     }
//
//
//     @SuppressAjWarnings("adviceDidNotMatch")
//     Object around(): asyncMethod() {
//         final MethodSignature methodSignature = (MethodSignature) thisJoinPointStaticPart.getSignature();
//         AsyncTaskExecutor executor = determineAsyncExecutor(methodSignature.getMethod());
//         if (executor == null) {
//             return proceed();
//         }
//         Callable<Object> task = () -> {
//             try {
//                 Object result = proceed();
//                 if (result instanceof Future) {
//                     return ((Future<?>) result).get();
//                 }
//             } catch (Throwable ex) {
//                 handleError(ex, methodSignature.getMethod(), thisJoinPoint.getArgs());
//             }
//             return null;
//         };
//         return doSubmit(task, executor, methodSignature.getReturnType());
//     }
//
//
//     public abstract pointcut asyncMethod();
//
// }
