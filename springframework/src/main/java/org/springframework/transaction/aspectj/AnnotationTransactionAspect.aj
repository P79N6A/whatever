package org.springframework.transaction.aspectj;

import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

public aspect AnnotationTransactionAspect extends AbstractTransactionAspect {

    public AnnotationTransactionAspect() {
        super(new AnnotationTransactionAttributeSource(false));
    }


    private pointcut executionOfAnyPublicMethodInAtTransactionalType():
            execution(public * ((@Transactional *)+).*(..)) && within(@Transactional *);


    private pointcut executionOfTransactionalMethod():
            execution(@Transactional * *(..));


    protected pointcut transactionalMethodExecution(Object txObject):
            (executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod() ) && this(txObject);

}
