package org.springframework.context.expression;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class BeanFactoryAccessor implements PropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[]{BeanFactory.class};
    }

    @Override
    public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return (target instanceof BeanFactory && ((BeanFactory) target).containsBean(name));
    }

    @Override
    public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        Assert.state(target instanceof BeanFactory, "Target must be of type BeanFactory");
        return new TypedValue(((BeanFactory) target).getBean(name));
    }

    @Override
    public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) throws AccessException {
        throw new AccessException("Beans in a BeanFactory are read-only");
    }

}
