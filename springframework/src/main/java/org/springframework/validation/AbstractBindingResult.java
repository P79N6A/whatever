package org.springframework.validation;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditor;
import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public abstract class AbstractBindingResult extends AbstractErrors implements BindingResult, Serializable {

    private final String objectName;

    private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();

    private final List<ObjectError> errors = new LinkedList<>();

    private final Map<String, Class<?>> fieldTypes = new HashMap<>();

    private final Map<String, Object> fieldValues = new HashMap<>();

    private final Set<String> suppressedFields = new HashSet<>();

    protected AbstractBindingResult(String objectName) {
        this.objectName = objectName;
    }

    public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
        Assert.notNull(messageCodesResolver, "MessageCodesResolver must not be null");
        this.messageCodesResolver = messageCodesResolver;
    }

    public MessageCodesResolver getMessageCodesResolver() {
        return this.messageCodesResolver;
    }
    //---------------------------------------------------------------------
    // Implementation of the Errors interface
    //---------------------------------------------------------------------

    @Override
    public String getObjectName() {
        return this.objectName;
    }

    @Override
    public void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
        addError(new ObjectError(getObjectName(), resolveMessageCodes(errorCode), errorArgs, defaultMessage));
    }

    @Override
    public void rejectValue(@Nullable String field, String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage) {
        if ("".equals(getNestedPath()) && !StringUtils.hasLength(field)) {
            // We're at the top of the nested object hierarchy,
            // so the present level is not a field but rather the top object.
            // The best we can do is register a global error here...
            reject(errorCode, errorArgs, defaultMessage);
            return;
        }
        String fixedField = fixedField(field);
        Object newVal = getActualFieldValue(fixedField);
        FieldError fe = new FieldError(getObjectName(), fixedField, newVal, false, resolveMessageCodes(errorCode, field), errorArgs, defaultMessage);
        addError(fe);
    }

    @Override
    public void addAllErrors(Errors errors) {
        if (!errors.getObjectName().equals(getObjectName())) {
            throw new IllegalArgumentException("Errors object needs to have same object name");
        }
        this.errors.addAll(errors.getAllErrors());
    }

    @Override
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    @Override
    public int getErrorCount() {
        return this.errors.size();
    }

    @Override
    public List<ObjectError> getAllErrors() {
        return Collections.unmodifiableList(this.errors);
    }

    @Override
    public List<ObjectError> getGlobalErrors() {
        List<ObjectError> result = new LinkedList<>();
        for (ObjectError objectError : this.errors) {
            if (!(objectError instanceof FieldError)) {
                result.add(objectError);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    @Nullable
    public ObjectError getGlobalError() {
        for (ObjectError objectError : this.errors) {
            if (!(objectError instanceof FieldError)) {
                return objectError;
            }
        }
        return null;
    }

    @Override
    public List<FieldError> getFieldErrors() {
        List<FieldError> result = new LinkedList<>();
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError) {
                result.add((FieldError) objectError);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    @Nullable
    public FieldError getFieldError() {
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError) {
                return (FieldError) objectError;
            }
        }
        return null;
    }

    @Override
    public List<FieldError> getFieldErrors(String field) {
        List<FieldError> result = new LinkedList<>();
        String fixedField = fixedField(field);
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError && isMatchingFieldError(fixedField, (FieldError) objectError)) {
                result.add((FieldError) objectError);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    @Nullable
    public FieldError getFieldError(String field) {
        String fixedField = fixedField(field);
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError) {
                FieldError fieldError = (FieldError) objectError;
                if (isMatchingFieldError(fixedField, fieldError)) {
                    return fieldError;
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Object getFieldValue(String field) {
        FieldError fieldError = getFieldError(field);
        // Use rejected value in case of error, current field value otherwise.
        if (fieldError != null) {
            Object value = fieldError.getRejectedValue();
            // Do not apply formatting on binding failures like type mismatches.
            return (fieldError.isBindingFailure() || getTarget() == null ? value : formatFieldValue(field, value));
        } else if (getTarget() != null) {
            Object value = getActualFieldValue(fixedField(field));
            return formatFieldValue(field, value);
        } else {
            return this.fieldValues.get(field);
        }
    }

    @Override
    @Nullable
    public Class<?> getFieldType(@Nullable String field) {
        if (getTarget() != null) {
            Object value = getActualFieldValue(fixedField(field));
            if (value != null) {
                return value.getClass();
            }
        }
        return this.fieldTypes.get(field);
    }
    //---------------------------------------------------------------------
    // Implementation of BindingResult interface
    //---------------------------------------------------------------------

    @Override
    public Map<String, Object> getModel() {
        Map<String, Object> model = new LinkedHashMap<>(2);
        // Mapping from name to target object.
        model.put(getObjectName(), getTarget());
        // Errors instance, even if no errors.
        model.put(MODEL_KEY_PREFIX + getObjectName(), this);
        return model;
    }

    @Override
    @Nullable
    public Object getRawFieldValue(String field) {
        return (getTarget() != null ? getActualFieldValue(fixedField(field)) : null);
    }

    @Override
    @Nullable
    public PropertyEditor findEditor(@Nullable String field, @Nullable Class<?> valueType) {
        PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();
        if (editorRegistry != null) {
            Class<?> valueTypeToUse = valueType;
            if (valueTypeToUse == null) {
                valueTypeToUse = getFieldType(field);
            }
            return editorRegistry.findCustomEditor(valueTypeToUse, fixedField(field));
        } else {
            return null;
        }
    }

    @Override
    @Nullable
    public PropertyEditorRegistry getPropertyEditorRegistry() {
        return null;
    }

    @Override
    public String[] resolveMessageCodes(String errorCode) {
        return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
    }

    @Override
    public String[] resolveMessageCodes(String errorCode, @Nullable String field) {
        return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName(), fixedField(field), getFieldType(field));
    }

    @Override
    public void addError(ObjectError error) {
        this.errors.add(error);
    }

    @Override
    public void recordFieldValue(String field, Class<?> type, @Nullable Object value) {
        this.fieldTypes.put(field, type);
        this.fieldValues.put(field, value);
    }

    @Override
    public void recordSuppressedField(String field) {
        this.suppressedFields.add(field);
    }

    @Override
    public String[] getSuppressedFields() {
        return StringUtils.toStringArray(this.suppressedFields);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BindingResult)) {
            return false;
        }
        BindingResult otherResult = (BindingResult) other;
        return (getObjectName().equals(otherResult.getObjectName()) && ObjectUtils.nullSafeEquals(getTarget(), otherResult.getTarget()) && getAllErrors().equals(otherResult.getAllErrors()));
    }

    @Override
    public int hashCode() {
        return getObjectName().hashCode();
    }
    //---------------------------------------------------------------------
    // Template methods to be implemented/overridden by subclasses
    //---------------------------------------------------------------------

    @Override
    @Nullable
    public abstract Object getTarget();

    @Nullable
    protected abstract Object getActualFieldValue(String field);

    @Nullable
    protected Object formatFieldValue(String field, @Nullable Object value) {
        return value;
    }

}
