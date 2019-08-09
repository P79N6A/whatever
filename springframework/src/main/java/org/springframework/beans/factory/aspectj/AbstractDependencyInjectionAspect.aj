// package org.springframework.beans.factory.aspectj;
//
// import org.aspectj.lang.annotation.SuppressAjWarnings;
// import org.aspectj.lang.annotation.control.CodeGenerationHint;
//
// public abstract aspect AbstractDependencyInjectionAspect {
//
//     private pointcut preConstructionCondition():
//             leastSpecificSuperTypeConstruction() && preConstructionConfiguration();
//
//     private pointcut postConstructionCondition():
//             mostSpecificSubTypeConstruction() && !preConstructionConfiguration();
//
//
//     public abstract pointcut leastSpecificSuperTypeConstruction();
//
//
//     @CodeGenerationHint(ifNameSuffix = "6f1") public pointcut mostSpecificSubTypeConstruction():
//             if (thisJoinPoint.getSignature().getDeclaringType() == thisJoinPoint.getThis().getClass());
//
//
//     public pointcut preConstructionConfiguration(): if (false);
//
//
//     public abstract pointcut beanConstruction(Object bean);
//
//
//     public abstract pointcut beanDeserialization(Object bean);
//
//
//     public abstract pointcut inConfigurableBean();
//
//
//
//     @SuppressAjWarnings("adviceDidNotMatch")
//     before(Object bean):
//             beanConstruction(bean) && preConstructionCondition() && inConfigurableBean()  {
//         configureBean(bean);
//     }
//
//
//     @SuppressAjWarnings("adviceDidNotMatch")
//     after(Object bean) returning :
//             beanConstruction(bean) && postConstructionCondition() && inConfigurableBean() {
//         configureBean(bean);
//     }
//
//
//     @SuppressAjWarnings("adviceDidNotMatch")
//     after(Object bean) returning :
//             beanDeserialization(bean) && inConfigurableBean() {
//         configureBean(bean);
//     }
//
//
//
//     public abstract void configureBean(Object bean);
//
// }
