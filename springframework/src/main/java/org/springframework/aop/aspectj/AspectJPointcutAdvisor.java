package org.springframework.aop.aspectj;

import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class AspectJPointcutAdvisor implements PointcutAdvisor, Ordered {

    private final AbstractAspectJAdvice advice;

    private final Pointcut pointcut;

    @Nullable
    private Integer order;

    public AspectJPointcutAdvisor(AbstractAspectJAdvice advice) {
        Assert.notNull(advice, "Advice must not be null");
        this.advice = advice;
        this.pointcut = advice.buildSafePointcut();
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        if (this.order != null) {
            return this.order;
        } else {
            return this.advice.getOrder();
        }
    }

    @Override
    public boolean isPerInstance() {
        return true;
    }

    @Override
    public Advice getAdvice() {
        return this.advice;
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    public String getAspectName() {
        return this.advice.getAspectName();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AspectJPointcutAdvisor)) {
            return false;
        }
        AspectJPointcutAdvisor otherAdvisor = (AspectJPointcutAdvisor) other;
        return this.advice.equals(otherAdvisor.advice);
    }

    @Override
    public int hashCode() {
        return AspectJPointcutAdvisor.class.hashCode() * 29 + this.advice.hashCode();
    }

}
