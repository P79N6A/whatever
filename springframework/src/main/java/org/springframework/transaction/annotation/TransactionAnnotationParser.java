package org.springframework.transaction.annotation;

import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.lang.reflect.AnnotatedElement;

public interface TransactionAnnotationParser {

    default boolean isCandidateClass(Class<?> targetClass) {
        return true;
    }

    @Nullable
    TransactionAttribute parseTransactionAnnotation(AnnotatedElement element);

}
