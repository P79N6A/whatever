// package org.springframework.scheduling.aspectj;
//
// import org.springframework.core.annotation.AnnotatedElementUtils;
// import org.springframework.scheduling.annotation.Async;
//
// import java.lang.reflect.Method;
//
// public aspect AnnotationAsyncExecutionAspect extends AbstractAsyncExecutionAspect {
//
//     private pointcut asyncMarkedMethod(): execution(@Async (void || Future+) *(..));
//
//     private pointcut asyncTypeMarkedMethod(): execution((void || Future+) (@Async *).*(..));
//
//     public pointcut asyncMethod(): asyncMarkedMethod() || asyncTypeMarkedMethod();
//
//
//
//     @Override
//     protected String getExecutorQualifier(Method method) {
//         // Maintainer's note: changes made here should also be made in
//         // AnnotationAsyncExecutionInterceptor#getExecutorQualifier
//         Async async = AnnotatedElementUtils.findMergedAnnotation(method, Async.class);
//         if (async == null) {
//             async = AnnotatedElementUtils.findMergedAnnotation(method.getDeclaringClass(), Async.class);
//         }
//         return (async != null ? async.value() : null);
//     }
//
//
//     declare error:
//             execution(@Async !(void || Future+) *(..)):
//             "Only methods that return void or Future may have an @Async annotation";
//
//     declare warning:
//             execution(!(void || Future+) (@Async *).*(..)):
//             "Methods in a class marked with @Async that do not return void or Future will be routed synchronously";
//
// }
