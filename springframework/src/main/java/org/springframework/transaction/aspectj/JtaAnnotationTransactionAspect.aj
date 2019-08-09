package org.springframework.transaction.aspectj;

import org.aspectj.lang.annotation.RequiredTypes;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;

@RequiredTypes("javax.transaction.Transactional") public aspect JtaAnnotationTransactionAspect extends AbstractTransactionAspect {

    public JtaAnnotationTransactionAspect() {
        super(new AnnotationTransactionAttributeSource(false));
    }


    private pointcut executionOfAnyPublicMethodInAtTransactionalType():
            execution(public * ((@Transactional *)+).*(..)) && within(@Transactional *);


    private pointcut executionOfTransactionalMethod():
            execution(@Transactional * *(..));


    protected pointcut transactionalMethodExecution(Object txObject):
            (executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod() ) && this(txObject);

}
