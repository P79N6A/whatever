package org.springframework.boot.autoconfigure.condition;

import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Order(Ordered.LOWEST_PRECEDENCE - 20)
class OnExpressionCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String expression = (String) metadata.getAnnotationAttributes(ConditionalOnExpression.class.getName()).get("value");
        expression = wrapIfNecessary(expression);
        ConditionMessage.Builder messageBuilder = ConditionMessage.forCondition(ConditionalOnExpression.class, "(" + expression + ")");
        expression = context.getEnvironment().resolvePlaceholders(expression);
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        if (beanFactory != null) {
            boolean result = evaluateExpression(beanFactory, expression);
            return new ConditionOutcome(result, messageBuilder.resultedIn(result));
        }
        return ConditionOutcome.noMatch(messageBuilder.because("no BeanFactory available."));
    }

    private Boolean evaluateExpression(ConfigurableListableBeanFactory beanFactory, String expression) {
        BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
        if (resolver == null) {
            resolver = new StandardBeanExpressionResolver();
        }
        BeanExpressionContext expressionContext = new BeanExpressionContext(beanFactory, null);
        Object result = resolver.evaluate(expression, expressionContext);
        return (result != null && (boolean) result);
    }

    private String wrapIfNecessary(String expression) {
        if (!expression.startsWith("#{")) {
            return "#{" + expression + "}";
        }
        return expression;
    }

}
