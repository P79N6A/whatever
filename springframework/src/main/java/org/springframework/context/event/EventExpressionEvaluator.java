package org.springframework.context.event;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class EventExpressionEvaluator extends CachedExpressionEvaluator {

    private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

    public boolean condition(String conditionExpression, ApplicationEvent event, Method targetMethod, AnnotatedElementKey methodKey, Object[] args, @Nullable BeanFactory beanFactory) {
        EventExpressionRootObject root = new EventExpressionRootObject(event, args);
        MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(root, targetMethod, args, getParameterNameDiscoverer());
        if (beanFactory != null) {
            evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
        }
        return (Boolean.TRUE.equals(getExpression(this.conditionCache, methodKey, conditionExpression).getValue(evaluationContext, Boolean.class)));
    }

}
