package org.springframework.web.bind;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.core.CollectionFactory;
import org.springframework.lang.Nullable;
import org.springframework.validation.DataBinder;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WebDataBinder extends DataBinder {

    public static final String DEFAULT_FIELD_MARKER_PREFIX = "_";

    public static final String DEFAULT_FIELD_DEFAULT_PREFIX = "!";

    @Nullable
    private String fieldMarkerPrefix = DEFAULT_FIELD_MARKER_PREFIX;

    @Nullable
    private String fieldDefaultPrefix = DEFAULT_FIELD_DEFAULT_PREFIX;

    private boolean bindEmptyMultipartFiles = true;

    public WebDataBinder(@Nullable Object target) {
        super(target);
    }

    public WebDataBinder(@Nullable Object target, String objectName) {
        super(target, objectName);
    }

    public void setFieldMarkerPrefix(@Nullable String fieldMarkerPrefix) {
        this.fieldMarkerPrefix = fieldMarkerPrefix;
    }

    @Nullable
    public String getFieldMarkerPrefix() {
        return this.fieldMarkerPrefix;
    }

    public void setFieldDefaultPrefix(@Nullable String fieldDefaultPrefix) {
        this.fieldDefaultPrefix = fieldDefaultPrefix;
    }

    @Nullable
    public String getFieldDefaultPrefix() {
        return this.fieldDefaultPrefix;
    }

    public void setBindEmptyMultipartFiles(boolean bindEmptyMultipartFiles) {
        this.bindEmptyMultipartFiles = bindEmptyMultipartFiles;
    }

    public boolean isBindEmptyMultipartFiles() {
        return this.bindEmptyMultipartFiles;
    }

    @Override
    protected void doBind(MutablePropertyValues mpvs) {
        checkFieldDefaults(mpvs);
        checkFieldMarkers(mpvs);
        super.doBind(mpvs);
    }

    protected void checkFieldDefaults(MutablePropertyValues mpvs) {
        String fieldDefaultPrefix = getFieldDefaultPrefix();
        if (fieldDefaultPrefix != null) {
            PropertyValue[] pvArray = mpvs.getPropertyValues();
            for (PropertyValue pv : pvArray) {
                if (pv.getName().startsWith(fieldDefaultPrefix)) {
                    String field = pv.getName().substring(fieldDefaultPrefix.length());
                    if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
                        mpvs.add(field, pv.getValue());
                    }
                    mpvs.removePropertyValue(pv);
                }
            }
        }
    }

    protected void checkFieldMarkers(MutablePropertyValues mpvs) {
        String fieldMarkerPrefix = getFieldMarkerPrefix();
        if (fieldMarkerPrefix != null) {
            PropertyValue[] pvArray = mpvs.getPropertyValues();
            for (PropertyValue pv : pvArray) {
                if (pv.getName().startsWith(fieldMarkerPrefix)) {
                    String field = pv.getName().substring(fieldMarkerPrefix.length());
                    if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
                        Class<?> fieldType = getPropertyAccessor().getPropertyType(field);
                        mpvs.add(field, getEmptyValue(field, fieldType));
                    }
                    mpvs.removePropertyValue(pv);
                }
            }
        }
    }

    @Nullable
    protected Object getEmptyValue(String field, @Nullable Class<?> fieldType) {
        return (fieldType != null ? getEmptyValue(fieldType) : null);
    }

    @Nullable
    public Object getEmptyValue(Class<?> fieldType) {
        try {
            if (boolean.class == fieldType || Boolean.class == fieldType) {
                // Special handling of boolean property.
                return Boolean.FALSE;
            } else if (fieldType.isArray()) {
                // Special handling of array property.
                return Array.newInstance(fieldType.getComponentType(), 0);
            } else if (Collection.class.isAssignableFrom(fieldType)) {
                return CollectionFactory.createCollection(fieldType, 0);
            } else if (Map.class.isAssignableFrom(fieldType)) {
                return CollectionFactory.createMap(fieldType, 0);
            }
        } catch (IllegalArgumentException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to create default value - falling back to null: " + ex.getMessage());
            }
        }
        // Default value: null.
        return null;
    }

    protected void bindMultipart(Map<String, List<MultipartFile>> multipartFiles, MutablePropertyValues mpvs) {
        multipartFiles.forEach((key, values) -> {
            if (values.size() == 1) {
                MultipartFile value = values.get(0);
                if (isBindEmptyMultipartFiles() || !value.isEmpty()) {
                    mpvs.add(key, value);
                }
            } else {
                mpvs.add(key, values);
            }
        });
    }

}
