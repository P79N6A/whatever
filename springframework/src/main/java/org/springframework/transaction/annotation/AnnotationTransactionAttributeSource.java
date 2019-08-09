package org.springframework.transaction.annotation;

import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.AbstractFallbackTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings("serial")
public class AnnotationTransactionAttributeSource extends AbstractFallbackTransactionAttributeSource implements Serializable {

    private static final boolean jta12Present;

    private static final boolean ejb3Present;

    static {
        ClassLoader classLoader = AnnotationTransactionAttributeSource.class.getClassLoader();
        jta12Present = ClassUtils.isPresent("javax.transaction.Transactional", classLoader);
        ejb3Present = ClassUtils.isPresent("javax.ejb.TransactionAttribute", classLoader);
    }

    private final boolean publicMethodsOnly;

    private final Set<TransactionAnnotationParser> annotationParsers;

    public AnnotationTransactionAttributeSource() {
        this(true);
    }

    public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
        if (jta12Present || ejb3Present) {
            this.annotationParsers = new LinkedHashSet<>(4);
            // 处理@org.springframework.transaction.annotation.Transactional
            this.annotationParsers.add(new SpringTransactionAnnotationParser());
            if (jta12Present) {
                // 处理@javax.transaction.Transactional
                this.annotationParsers.add(new JtaTransactionAnnotationParser());
            }
            if (ejb3Present) {
                // this.annotationParsers.add(new Ejb3TransactionAnnotationParser());
            }
        } else {
            this.annotationParsers = Collections.singleton(new SpringTransactionAnnotationParser());
        }
    }

    public AnnotationTransactionAttributeSource(TransactionAnnotationParser annotationParser) {
        this.publicMethodsOnly = true;
        Assert.notNull(annotationParser, "TransactionAnnotationParser must not be null");
        this.annotationParsers = Collections.singleton(annotationParser);
    }

    public AnnotationTransactionAttributeSource(TransactionAnnotationParser... annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
        this.annotationParsers = new LinkedHashSet<>(Arrays.asList(annotationParsers));
    }

    public AnnotationTransactionAttributeSource(Set<TransactionAnnotationParser> annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one TransactionAnnotationParser needs to be specified");
        this.annotationParsers = annotationParsers;
    }

    @Override
    public boolean isCandidateClass(Class<?> targetClass) {
        for (TransactionAnnotationParser parser : this.annotationParsers) {
            if (parser.isCandidateClass(targetClass)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    protected TransactionAttribute findTransactionAttribute(Class<?> clazz) {
        return determineTransactionAttribute(clazz);
    }

    @Override
    @Nullable
    protected TransactionAttribute findTransactionAttribute(Method method) {
        return determineTransactionAttribute(method);
    }

    @Nullable
    protected TransactionAttribute determineTransactionAttribute(AnnotatedElement element) {
        for (TransactionAnnotationParser parser : this.annotationParsers) {
            TransactionAttribute attr = parser.parseTransactionAnnotation(element);
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

    @Override
    protected boolean allowPublicMethodsOnly() {
        return this.publicMethodsOnly;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnnotationTransactionAttributeSource)) {
            return false;
        }
        AnnotationTransactionAttributeSource otherTas = (AnnotationTransactionAttributeSource) other;
        return (this.annotationParsers.equals(otherTas.annotationParsers) && this.publicMethodsOnly == otherTas.publicMethodsOnly);
    }

    @Override
    public int hashCode() {
        return this.annotationParsers.hashCode();
    }

}
