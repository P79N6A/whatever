package org.springframework.validation.beanvalidation;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.validation.*;

import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.ValidationException;
import javax.validation.executable.ExecutableValidator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import java.io.Serializable;
import java.util.*;

public class SpringValidatorAdapter implements SmartValidator, javax.validation.Validator {

    private static final Set<String> internalAnnotationAttributes = new HashSet<>(4);

    static {
        internalAnnotationAttributes.add("message");
        internalAnnotationAttributes.add("groups");
        internalAnnotationAttributes.add("payload");
    }

    @Nullable
    private javax.validation.Validator targetValidator;

    public SpringValidatorAdapter(javax.validation.Validator targetValidator) {
        Assert.notNull(targetValidator, "Target Validator must not be null");
        this.targetValidator = targetValidator;
    }

    SpringValidatorAdapter() {
    }

    void setTargetValidator(javax.validation.Validator targetValidator) {
        this.targetValidator = targetValidator;
    }
    //---------------------------------------------------------------------
    // Implementation of Spring Validator interface
    //---------------------------------------------------------------------

    @Override
    public boolean supports(Class<?> clazz) {
        return (this.targetValidator != null);
    }

    @Override
    public void validate(Object target, Errors errors) {
        if (this.targetValidator != null) {
            processConstraintViolations(this.targetValidator.validate(target), errors);
        }
    }

    @Override
    public void validate(Object target, Errors errors, Object... validationHints) {
        if (this.targetValidator != null) {
            processConstraintViolations(this.targetValidator.validate(target, asValidationGroups(validationHints)), errors);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void validateValue(Class<?> targetType, String fieldName, @Nullable Object value, Errors errors, Object... validationHints) {
        if (this.targetValidator != null) {
            processConstraintViolations(this.targetValidator.validateValue((Class) targetType, fieldName, value, asValidationGroups(validationHints)), errors);
        }
    }

    private Class<?>[] asValidationGroups(Object... validationHints) {
        Set<Class<?>> groups = new LinkedHashSet<>(4);
        for (Object hint : validationHints) {
            if (hint instanceof Class) {
                groups.add((Class<?>) hint);
            }
        }
        return ClassUtils.toClassArray(groups);
    }

    @SuppressWarnings("serial")
    protected void processConstraintViolations(Set<ConstraintViolation<Object>> violations, Errors errors) {
        for (ConstraintViolation<Object> violation : violations) {
            String field = determineField(violation);
            FieldError fieldError = errors.getFieldError(field);
            if (fieldError == null || !fieldError.isBindingFailure()) {
                try {
                    ConstraintDescriptor<?> cd = violation.getConstraintDescriptor();
                    String errorCode = determineErrorCode(cd);
                    Object[] errorArgs = getArgumentsForConstraint(errors.getObjectName(), field, cd);
                    if (errors instanceof BindingResult) {
                        // Can do custom FieldError registration with invalid value from ConstraintViolation,
                        // as necessary for Hibernate Validator compatibility (non-indexed set path in field)
                        BindingResult bindingResult = (BindingResult) errors;
                        String nestedField = bindingResult.getNestedPath() + field;
                        if (nestedField.isEmpty()) {
                            String[] errorCodes = bindingResult.resolveMessageCodes(errorCode);
                            ObjectError error = new ObjectError(errors.getObjectName(), errorCodes, errorArgs, violation.getMessage()) {
                                @Override
                                public boolean shouldRenderDefaultMessage() {
                                    return false;
                                }
                            };
                            error.wrap(violation);
                            bindingResult.addError(error);
                        } else {
                            Object rejectedValue = getRejectedValue(field, violation, bindingResult);
                            String[] errorCodes = bindingResult.resolveMessageCodes(errorCode, field);
                            FieldError error = new FieldError(errors.getObjectName(), nestedField, rejectedValue, false, errorCodes, errorArgs, violation.getMessage()) {
                                @Override
                                public boolean shouldRenderDefaultMessage() {
                                    return false;
                                }
                            };
                            error.wrap(violation);
                            bindingResult.addError(error);
                        }
                    } else {
                        // got no BindingResult - can only do standard rejectValue call
                        // with automatic extraction of the current field value
                        errors.rejectValue(field, errorCode, errorArgs, violation.getMessage());
                    }
                } catch (NotReadablePropertyException ex) {
                    throw new IllegalStateException("JSR-303 validated property '" + field + "' does not have a corresponding accessor for Spring data binding - " + "check your DataBinder's configuration (bean property versus direct field access)", ex);
                }
            }
        }
    }

    protected String determineField(ConstraintViolation<Object> violation) {
        Path path = violation.getPropertyPath();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Path.Node node : path) {
            if (node.isInIterable()) {
                sb.append('[');
                Object index = node.getIndex();
                if (index == null) {
                    index = node.getKey();
                }
                if (index != null) {
                    sb.append(index);
                }
                sb.append(']');
            }
            String name = node.getName();
            if (name != null && node.getKind() == ElementKind.PROPERTY && !name.startsWith("<")) {
                if (!first) {
                    sb.append('.');
                }
                first = false;
                sb.append(name);
            }
        }
        return sb.toString();
    }

    protected String determineErrorCode(ConstraintDescriptor<?> descriptor) {
        return descriptor.getAnnotation().annotationType().getSimpleName();
    }

    protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintDescriptor<?> descriptor) {
        List<Object> arguments = new ArrayList<>();
        arguments.add(getResolvableField(objectName, field));
        // Using a TreeMap for alphabetical ordering of attribute names
        Map<String, Object> attributesToExpose = new TreeMap<>();
        descriptor.getAttributes().forEach((attributeName, attributeValue) -> {
            if (!internalAnnotationAttributes.contains(attributeName)) {
                if (attributeValue instanceof String) {
                    attributeValue = new ResolvableAttribute(attributeValue.toString());
                }
                attributesToExpose.put(attributeName, attributeValue);
            }
        });
        arguments.addAll(attributesToExpose.values());
        return arguments.toArray();
    }

    protected MessageSourceResolvable getResolvableField(String objectName, String field) {
        String[] codes = new String[]{objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
        return new DefaultMessageSourceResolvable(codes, field);
    }

    @Nullable
    protected Object getRejectedValue(String field, ConstraintViolation<Object> violation, BindingResult bindingResult) {
        Object invalidValue = violation.getInvalidValue();
        if (!"".equals(field) && !field.contains("[]") && (invalidValue == violation.getLeafBean() || field.contains("[") || field.contains("."))) {
            // Possibly a bean constraint with property path: retrieve the actual property value.
            // However, explicitly avoid this for "address[]" style paths that we can't handle.
            invalidValue = bindingResult.getRawFieldValue(field);
        }
        return invalidValue;
    }
    //---------------------------------------------------------------------
    // Implementation of JSR-303 Validator interface
    //---------------------------------------------------------------------

    @Override
    public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
        Assert.state(this.targetValidator != null, "No target Validator set");
        return this.targetValidator.validate(object, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
        Assert.state(this.targetValidator != null, "No target Validator set");
        return this.targetValidator.validateProperty(object, propertyName, groups);
    }

    @Override
    public <T> Set<ConstraintViolation<T>> validateValue(Class<T> beanType, String propertyName, Object value, Class<?>... groups) {
        Assert.state(this.targetValidator != null, "No target Validator set");
        return this.targetValidator.validateValue(beanType, propertyName, value, groups);
    }

    @Override
    public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
        Assert.state(this.targetValidator != null, "No target Validator set");
        return this.targetValidator.getConstraintsForClass(clazz);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(@Nullable Class<T> type) {
        Assert.state(this.targetValidator != null, "No target Validator set");
        try {
            return (type != null ? this.targetValidator.unwrap(type) : (T) this.targetValidator);
        } catch (ValidationException ex) {
            // ignore if just being asked for plain Validator
            if (javax.validation.Validator.class == type) {
                return (T) this.targetValidator;
            }
            throw ex;
        }
    }

    @Override
    public ExecutableValidator forExecutables() {
        Assert.state(this.targetValidator != null, "No target Validator set");
        return this.targetValidator.forExecutables();
    }

    @SuppressWarnings("serial")
    private static class ResolvableAttribute implements MessageSourceResolvable, Serializable {

        private final String resolvableString;

        public ResolvableAttribute(String resolvableString) {
            this.resolvableString = resolvableString;
        }

        @Override
        public String[] getCodes() {
            return new String[]{this.resolvableString};
        }

        @Override
        @Nullable
        public Object[] getArguments() {
            return null;
        }

        @Override
        public String getDefaultMessage() {
            return this.resolvableString;
        }

    }

}
