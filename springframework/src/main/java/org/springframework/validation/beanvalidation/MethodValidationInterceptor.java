package org.springframework.validation.beanvalidation;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.SmartFactoryBean;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.*;
import javax.validation.executable.ExecutableValidator;
import java.lang.reflect.Method;
import java.util.Set;

public class MethodValidationInterceptor implements MethodInterceptor {

    private final Validator validator;

    public MethodValidationInterceptor() {
        this(Validation.buildDefaultValidatorFactory());
    }

    public MethodValidationInterceptor(ValidatorFactory validatorFactory) {
        this(validatorFactory.getValidator());
    }

    public MethodValidationInterceptor(Validator validator) {
        this.validator = validator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // Avoid Validator invocation on FactoryBean.getObjectType/isSingleton
        if (isFactoryBeanMetadataMethod(invocation.getMethod())) {
            return invocation.proceed();
        }
        Class<?>[] groups = determineValidationGroups(invocation);
        // Standard Bean Validation 1.1 API
        ExecutableValidator execVal = this.validator.forExecutables();
        Method methodToValidate = invocation.getMethod();
        Set<ConstraintViolation<Object>> result;
        try {
            result = execVal.validateParameters(invocation.getThis(), methodToValidate, invocation.getArguments(), groups);
        } catch (IllegalArgumentException ex) {
            // Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
            // Let's try to find the bridged method on the implementation class...
            methodToValidate = BridgeMethodResolver.findBridgedMethod(ClassUtils.getMostSpecificMethod(invocation.getMethod(), invocation.getThis().getClass()));
            result = execVal.validateParameters(invocation.getThis(), methodToValidate, invocation.getArguments(), groups);
        }
        if (!result.isEmpty()) {
            throw new ConstraintViolationException(result);
        }
        Object returnValue = invocation.proceed();
        result = execVal.validateReturnValue(invocation.getThis(), methodToValidate, returnValue, groups);
        if (!result.isEmpty()) {
            throw new ConstraintViolationException(result);
        }
        return returnValue;
    }

    private boolean isFactoryBeanMetadataMethod(Method method) {
        Class<?> clazz = method.getDeclaringClass();
        // Call from interface-based proxy handle, allowing for an efficient check?
        if (clazz.isInterface()) {
            return ((clazz == FactoryBean.class || clazz == SmartFactoryBean.class) && !method.getName().equals("getObject"));
        }
        // Call from CGLIB proxy handle, potentially implementing a FactoryBean method?
        Class<?> factoryBeanType = null;
        if (SmartFactoryBean.class.isAssignableFrom(clazz)) {
            factoryBeanType = SmartFactoryBean.class;
        } else if (FactoryBean.class.isAssignableFrom(clazz)) {
            factoryBeanType = FactoryBean.class;
        }
        return (factoryBeanType != null && !method.getName().equals("getObject") && ClassUtils.hasMethod(factoryBeanType, method.getName(), method.getParameterTypes()));
    }

    protected Class<?>[] determineValidationGroups(MethodInvocation invocation) {
        Validated validatedAnn = AnnotationUtils.findAnnotation(invocation.getMethod(), Validated.class);
        if (validatedAnn == null) {
            validatedAnn = AnnotationUtils.findAnnotation(invocation.getThis().getClass(), Validated.class);
        }
        return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
    }

}
