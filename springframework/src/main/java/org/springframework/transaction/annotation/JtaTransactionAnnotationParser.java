package org.springframework.transaction.annotation;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.interceptor.NoRollbackRuleAttribute;
import org.springframework.transaction.interceptor.RollbackRuleAttribute;
import org.springframework.transaction.interceptor.RuleBasedTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class JtaTransactionAnnotationParser implements TransactionAnnotationParser, Serializable {

    @Override
    public boolean isCandidateClass(Class<?> targetClass) {
        return AnnotationUtils.isCandidateClass(targetClass, javax.transaction.Transactional.class);
    }

    @Override
    @Nullable
    public TransactionAttribute parseTransactionAnnotation(AnnotatedElement element) {
        AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(element, javax.transaction.Transactional.class);
        if (attributes != null) {
            return parseTransactionAnnotation(attributes);
        } else {
            return null;
        }
    }

    public TransactionAttribute parseTransactionAnnotation(javax.transaction.Transactional ann) {
        return parseTransactionAnnotation(AnnotationUtils.getAnnotationAttributes(ann, false, false));
    }

    protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {
        RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
        rbta.setPropagationBehaviorName(RuleBasedTransactionAttribute.PREFIX_PROPAGATION + attributes.getEnum("value").toString());
        List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();
        for (Class<?> rbRule : attributes.getClassArray("rollbackOn")) {
            rollbackRules.add(new RollbackRuleAttribute(rbRule));
        }
        for (Class<?> rbRule : attributes.getClassArray("dontRollbackOn")) {
            rollbackRules.add(new NoRollbackRuleAttribute(rbRule));
        }
        rbta.setRollbackRules(rollbackRules);
        return rbta;
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || other instanceof JtaTransactionAnnotationParser);
    }

    @Override
    public int hashCode() {
        return JtaTransactionAnnotationParser.class.hashCode();
    }

}
