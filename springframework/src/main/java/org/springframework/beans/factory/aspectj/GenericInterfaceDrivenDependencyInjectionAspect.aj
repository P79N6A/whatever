// package org.springframework.beans.factory.aspectj;
//
// public abstract aspect GenericInterfaceDrivenDependencyInjectionAspect<I> extends AbstractInterfaceDrivenDependencyInjectionAspect {
//
//     declare parents:I implements ConfigurableObject;
//
//     public pointcut inConfigurableBean(): within(I+);
//
//     @SuppressWarnings("unchecked")
//     public final void configureBean(Object bean) {
//         configure((I) bean);
//     }
//
//     // Unfortunately, erasure used with generics won't allow to use the same named method
//     protected abstract void configure(I bean);
//
// }
