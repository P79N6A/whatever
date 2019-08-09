// package org.springframework.beans.factory.aspectj;
//
// import java.io.ObjectStreamException;
// import java.io.Serializable;
//
// public abstract aspect AbstractInterfaceDrivenDependencyInjectionAspect extends AbstractDependencyInjectionAspect {
//
//     public pointcut beanConstruction(Object bean):
//             initialization(ConfigurableObject+.new(..)) && this(bean);
//
//
//     public pointcut beanDeserialization(Object bean):
//             execution(Object ConfigurableDeserializationSupport+.readResolve()) && this(bean);
//
//     public pointcut leastSpecificSuperTypeConstruction(): initialization(ConfigurableObject.new(..));
//
//
//
//     // Implementation to support re-injecting dependencies once an object is deserialized
//
//
//     declare parents:ConfigurableObject+&&Serializable+implements ConfigurableDeserializationSupport;
//
//
//     static interface ConfigurableDeserializationSupport extends Serializable {
//     }
//
//     public Object ConfigurableDeserializationSupport.readResolve() throws ObjectStreamException {
//         return this;
//     }
//
// }
