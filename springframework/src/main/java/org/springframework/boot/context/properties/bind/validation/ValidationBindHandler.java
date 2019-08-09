package org.springframework.boot.context.properties.bind.validation;

import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;

import java.util.*;
import java.util.stream.Collectors;

public class ValidationBindHandler extends AbstractBindHandler {

    private final Validator[] validators;

    private final Set<ConfigurationProperty> boundProperties = new LinkedHashSet<>();

    private final Deque<BindValidationException> exceptions = new LinkedList<>();

    public ValidationBindHandler(Validator... validators) {
        this.validators = validators;
    }

    public ValidationBindHandler(BindHandler parent, Validator... validators) {
        super(parent);
        this.validators = validators;
    }

    @Override
    public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        if (context.getConfigurationProperty() != null) {
            this.boundProperties.add(context.getConfigurationProperty());
        }
        return super.onSuccess(name, target, context, result);
    }

    @Override
    public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
        validate(name, target, context, result);
        super.onFinish(name, target, context, result);
    }

    @Override
    public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error) throws Exception {
        Object result = super.onFailure(name, target, context, error);
        validate(name, target, context, null);
        return result;
    }

    private void validate(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        Object validationTarget = getValidationTarget(target, context, result);
        Class<?> validationType = target.getBoxedType().resolve();
        if (validationTarget != null) {
            validateAndPush(name, validationTarget, validationType);
        }
        if (context.getDepth() == 0 && !this.exceptions.isEmpty()) {
            throw this.exceptions.pop();
        }
    }

    private Object getValidationTarget(Bindable<?> target, BindContext context, Object result) {
        if (result != null) {
            return result;
        }
        if (context.getDepth() == 0 && target.getValue() != null) {
            return target.getValue().get();
        }
        return null;
    }

    private void validateAndPush(ConfigurationPropertyName name, Object target, Class<?> type) {
        BindingResult errors = new BeanPropertyBindingResult(target, name.toString());
        Arrays.stream(this.validators).filter((validator) -> validator.supports(type)).forEach((validator) -> validator.validate(target, errors));
        if (errors.hasErrors()) {
            this.exceptions.push(getBindValidationException(name, errors));
        }
    }

    private BindValidationException getBindValidationException(ConfigurationPropertyName name, BindingResult errors) {
        Set<ConfigurationProperty> boundProperties = this.boundProperties.stream().filter((property) -> name.isAncestorOf(property.getName())).collect(Collectors.toCollection(LinkedHashSet::new));
        ValidationErrors validationErrors = new ValidationErrors(name, boundProperties, errors.getAllErrors());
        return new BindValidationException(validationErrors);
    }

}
