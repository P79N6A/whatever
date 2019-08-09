// package org.springframework.transaction.annotation;
//
// import org.omg.CORBA.portable.ApplicationException;
// import org.springframework.core.annotation.AnnotationUtils;
// import org.springframework.lang.Nullable;
// import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
// import org.springframework.transaction.interceptor.TransactionAttribute;
//
// // import javax.ejb.ApplicationException;
// // import javax.ejb.TransactionAttributeType;
// import java.io.Serializable;
// import java.lang.reflect.AnnotatedElement;
//
// @SuppressWarnings("serial")
// public class Ejb3TransactionAnnotationParser implements TransactionAnnotationParser, Serializable {
//
//     @Override
//     public boolean isCandidateClass(Class<?> targetClass) {
//         return AnnotationUtils.isCandidateClass(targetClass, javax.ejb.TransactionAttribute.class);
//     }
//
//     @Override
//     @Nullable
//     public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
//         javax.ejb.TransactionAttribute ann = element.getAnnotation(javax.ejb.TransactionAttribute.class);
//         if (ann != null) {
//             return parseTransactionAnnotation(ann);
//         } else {
//             return null;
//         }
//     }
//
//     public TransactionAttribute parseTransactionAnnotation(javax.ejb.TransactionAttribute ann) {
//         return new Ejb3TransactionAttribute(ann.value());
//     }
//
//     @Override
//     public boolean equals(Object other) {
//         return (this == other || other instanceof Ejb3TransactionAnnotationParser);
//     }
//
//     @Override
//     public int hashCode() {
//         return Ejb3TransactionAnnotationParser.class.hashCode();
//     }
//
//     private static class Ejb3TransactionAttribute extends DefaultTransactionAttribute {
//
//         public Ejb3TransactionAttribute(TransactionAttributeType type) {
//             setPropagationBehaviorName(PREFIX_PROPAGATION + type.name());
//         }
//
//         @Override
//         public boolean rollbackOn(Throwable ex) {
//             ApplicationException ann = ex.getClass().getAnnotation(ApplicationException.class);
//             return (ann != null ? ann.rollback() : super.rollbackOn(ex));
//         }
//
//     }
//
// }
