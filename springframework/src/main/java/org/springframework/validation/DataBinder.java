package org.springframework.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.*;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.format.Formatter;
import org.springframework.format.support.FormatterPropertyEditorAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.util.*;

public class DataBinder implements PropertyEditorRegistry, TypeConverter {

    public static final String DEFAULT_OBJECT_NAME = "target";

    public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;

    protected static final Log logger = LogFactory.getLog(DataBinder.class);

    @Nullable
    private final Object target;

    private final String objectName;

    @Nullable
    private AbstractPropertyBindingResult bindingResult;

    @Nullable
    private SimpleTypeConverter typeConverter;

    private boolean ignoreUnknownFields = true;

    private boolean ignoreInvalidFields = false;

    private boolean autoGrowNestedPaths = true;

    private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;

    @Nullable
    private String[] allowedFields;

    @Nullable
    private String[] disallowedFields;

    @Nullable
    private String[] requiredFields;

    @Nullable
    private ConversionService conversionService;

    @Nullable
    private MessageCodesResolver messageCodesResolver;

    private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();

    private final List<Validator> validators = new ArrayList<>();

    public DataBinder(@Nullable Object target) {
        this(target, DEFAULT_OBJECT_NAME);
    }

    public DataBinder(@Nullable Object target, String objectName) {
        this.target = ObjectUtils.unwrapOptional(target);
        this.objectName = objectName;
    }

    @Nullable
    public Object getTarget() {
        return this.target;
    }

    public String getObjectName() {
        return this.objectName;
    }

    public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    public boolean isAutoGrowNestedPaths() {
        return this.autoGrowNestedPaths;
    }

    public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
        this.autoGrowCollectionLimit = autoGrowCollectionLimit;
    }

    public int getAutoGrowCollectionLimit() {
        return this.autoGrowCollectionLimit;
    }

    public void initBeanPropertyAccess() {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
        this.bindingResult = createBeanPropertyBindingResult();
    }

    protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(), getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());
        if (this.conversionService != null) {
            result.initConversion(this.conversionService);
        }
        if (this.messageCodesResolver != null) {
            result.setMessageCodesResolver(this.messageCodesResolver);
        }
        return result;
    }

    public void initDirectFieldAccess() {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
        this.bindingResult = createDirectFieldBindingResult();
    }

    protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
        DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(), getObjectName(), isAutoGrowNestedPaths());
        if (this.conversionService != null) {
            result.initConversion(this.conversionService);
        }
        if (this.messageCodesResolver != null) {
            result.setMessageCodesResolver(this.messageCodesResolver);
        }
        return result;
    }

    protected AbstractPropertyBindingResult getInternalBindingResult() {
        if (this.bindingResult == null) {
            initBeanPropertyAccess();
        }
        return this.bindingResult;
    }

    protected ConfigurablePropertyAccessor getPropertyAccessor() {
        return getInternalBindingResult().getPropertyAccessor();
    }

    protected SimpleTypeConverter getSimpleTypeConverter() {
        if (this.typeConverter == null) {
            this.typeConverter = new SimpleTypeConverter();
            if (this.conversionService != null) {
                this.typeConverter.setConversionService(this.conversionService);
            }
        }
        return this.typeConverter;
    }

    protected PropertyEditorRegistry getPropertyEditorRegistry() {
        if (getTarget() != null) {
            return getInternalBindingResult().getPropertyAccessor();
        } else {
            return getSimpleTypeConverter();
        }
    }

    protected TypeConverter getTypeConverter() {
        if (getTarget() != null) {
            return getInternalBindingResult().getPropertyAccessor();
        } else {
            return getSimpleTypeConverter();
        }
    }

    public BindingResult getBindingResult() {
        return getInternalBindingResult();
    }

    public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
        this.ignoreUnknownFields = ignoreUnknownFields;
    }

    public boolean isIgnoreUnknownFields() {
        return this.ignoreUnknownFields;
    }

    public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
        this.ignoreInvalidFields = ignoreInvalidFields;
    }

    public boolean isIgnoreInvalidFields() {
        return this.ignoreInvalidFields;
    }

    public void setAllowedFields(@Nullable String... allowedFields) {
        this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
    }

    @Nullable
    public String[] getAllowedFields() {
        return this.allowedFields;
    }

    public void setDisallowedFields(@Nullable String... disallowedFields) {
        this.disallowedFields = PropertyAccessorUtils.canonicalPropertyNames(disallowedFields);
    }

    @Nullable
    public String[] getDisallowedFields() {
        return this.disallowedFields;
    }

    public void setRequiredFields(@Nullable String... requiredFields) {
        this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
        if (logger.isDebugEnabled()) {
            logger.debug("DataBinder requires binding of required fields [" + StringUtils.arrayToCommaDelimitedString(requiredFields) + "]");
        }
    }

    @Nullable
    public String[] getRequiredFields() {
        return this.requiredFields;
    }

    public void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
        Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
        this.messageCodesResolver = messageCodesResolver;
        if (this.bindingResult != null && messageCodesResolver != null) {
            this.bindingResult.setMessageCodesResolver(messageCodesResolver);
        }
    }

    public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
        Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
        this.bindingErrorProcessor = bindingErrorProcessor;
    }

    public BindingErrorProcessor getBindingErrorProcessor() {
        return this.bindingErrorProcessor;
    }

    public void setValidator(@Nullable Validator validator) {
        assertValidators(validator);
        this.validators.clear();
        if (validator != null) {
            this.validators.add(validator);
        }
    }

    private void assertValidators(Validator... validators) {
        Object target = getTarget();
        for (Validator validator : validators) {
            if (validator != null && (target != null && !validator.supports(target.getClass()))) {
                throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
            }
        }
    }

    public void addValidators(Validator... validators) {
        assertValidators(validators);
        this.validators.addAll(Arrays.asList(validators));
    }

    public void replaceValidators(Validator... validators) {
        assertValidators(validators);
        this.validators.clear();
        this.validators.addAll(Arrays.asList(validators));
    }

    @Nullable
    public Validator getValidator() {
        return (!this.validators.isEmpty() ? this.validators.get(0) : null);
    }

    public List<Validator> getValidators() {
        return Collections.unmodifiableList(this.validators);
    }
    //---------------------------------------------------------------------
    // Implementation of PropertyEditorRegistry/TypeConverter interface
    //---------------------------------------------------------------------

    public void setConversionService(@Nullable ConversionService conversionService) {
        Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
        this.conversionService = conversionService;
        if (this.bindingResult != null && conversionService != null) {
            this.bindingResult.initConversion(conversionService);
        }
    }

    @Nullable
    public ConversionService getConversionService() {
        return this.conversionService;
    }

    public void addCustomFormatter(Formatter<?> formatter) {
        FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
        getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
    }

    public void addCustomFormatter(Formatter<?> formatter, String... fields) {
        FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
        Class<?> fieldType = adapter.getFieldType();
        if (ObjectUtils.isEmpty(fields)) {
            getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
        } else {
            for (String field : fields) {
                getPropertyEditorRegistry().registerCustomEditor(fieldType, field, adapter);
            }
        }
    }

    public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
        FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
        if (ObjectUtils.isEmpty(fieldTypes)) {
            getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
        } else {
            for (Class<?> fieldType : fieldTypes) {
                getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
            }
        }
    }

    @Override
    public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
    }

    @Override
    public void registerCustomEditor(@Nullable Class<?> requiredType, @Nullable String field, PropertyEditor propertyEditor) {
        getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
    }

    @Override
    @Nullable
    public PropertyEditor findCustomEditor(@Nullable Class<?> requiredType, @Nullable String propertyPath) {
        return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
    }

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType);
    }

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable MethodParameter methodParam) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
    }

    @Override
    @Nullable
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable Field field) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType, field);
    }

    @Nullable
    @Override
    public <T> T convertIfNecessary(@Nullable Object value, @Nullable Class<T> requiredType, @Nullable TypeDescriptor typeDescriptor) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType, typeDescriptor);
    }

    public void bind(PropertyValues pvs) {
        MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues ? (MutablePropertyValues) pvs : new MutablePropertyValues(pvs));
        doBind(mpvs);
    }

    protected void doBind(MutablePropertyValues mpvs) {
        checkAllowedFields(mpvs);
        checkRequiredFields(mpvs);
        applyPropertyValues(mpvs);
    }

    protected void checkAllowedFields(MutablePropertyValues mpvs) {
        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (PropertyValue pv : pvs) {
            String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
            if (!isAllowed(field)) {
                mpvs.removePropertyValue(pv);
                getBindingResult().recordSuppressedField(field);
                if (logger.isDebugEnabled()) {
                    logger.debug("Field [" + field + "] has been removed from PropertyValues " + "and will not be bound, because it has not been found in the list of allowed fields");
                }
            }
        }
    }

    protected boolean isAllowed(String field) {
        String[] allowed = getAllowedFields();
        String[] disallowed = getDisallowedFields();
        return ((ObjectUtils.isEmpty(allowed) || PatternMatchUtils.simpleMatch(allowed, field)) && (ObjectUtils.isEmpty(disallowed) || !PatternMatchUtils.simpleMatch(disallowed, field)));
    }

    protected void checkRequiredFields(MutablePropertyValues mpvs) {
        String[] requiredFields = getRequiredFields();
        if (!ObjectUtils.isEmpty(requiredFields)) {
            Map<String, PropertyValue> propertyValues = new HashMap<>();
            PropertyValue[] pvs = mpvs.getPropertyValues();
            for (PropertyValue pv : pvs) {
                String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
                propertyValues.put(canonicalName, pv);
            }
            for (String field : requiredFields) {
                PropertyValue pv = propertyValues.get(field);
                boolean empty = (pv == null || pv.getValue() == null);
                if (!empty) {
                    if (pv.getValue() instanceof String) {
                        empty = !StringUtils.hasText((String) pv.getValue());
                    } else if (pv.getValue() instanceof String[]) {
                        String[] values = (String[]) pv.getValue();
                        empty = (values.length == 0 || !StringUtils.hasText(values[0]));
                    }
                }
                if (empty) {
                    // Use bind error processor to create FieldError.
                    getBindingErrorProcessor().processMissingFieldError(field, getInternalBindingResult());
                    // Remove property from property values to bind:
                    // It has already caused a field error with a rejected value.
                    if (pv != null) {
                        mpvs.removePropertyValue(pv);
                        propertyValues.remove(field);
                    }
                }
            }
        }
    }

    protected void applyPropertyValues(MutablePropertyValues mpvs) {
        try {
            // Bind request parameters onto target object.
            getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
        } catch (PropertyBatchUpdateException ex) {
            // Use bind error processor to create FieldErrors.
            for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
                getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
            }
        }
    }

    public void validate() {
        Object target = getTarget();
        Assert.state(target != null, "No target to validate");
        BindingResult bindingResult = getBindingResult();
        // Call each validator with the same binding result
        for (Validator validator : getValidators()) {
            validator.validate(target, bindingResult);
        }
    }

    public void validate(Object... validationHints) {
        Object target = getTarget();
        Assert.state(target != null, "No target to validate");
        BindingResult bindingResult = getBindingResult();
        // Call each validator with the same binding result
        for (Validator validator : getValidators()) {
            if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
                ((SmartValidator) validator).validate(target, bindingResult, validationHints);
            } else if (validator != null) {
                validator.validate(target, bindingResult);
            }
        }
    }

    public Map<?, ?> close() throws BindException {
        if (getBindingResult().hasErrors()) {
            throw new BindException(getBindingResult());
        }
        return getBindingResult().getModel();
    }

}
