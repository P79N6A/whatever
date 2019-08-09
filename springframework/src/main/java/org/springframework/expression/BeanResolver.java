package org.springframework.expression;

public interface BeanResolver {

    Object resolve(EvaluationContext context, String beanName) throws AccessException;

}
