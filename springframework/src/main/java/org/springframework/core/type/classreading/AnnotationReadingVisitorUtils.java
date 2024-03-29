package org.springframework.core.type.classreading;

import org.springframework.asm.Type;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ObjectUtils;

import java.util.*;

@Deprecated
abstract class AnnotationReadingVisitorUtils {

    public static AnnotationAttributes convertClassValues(Object annotatedElement, @Nullable ClassLoader classLoader, AnnotationAttributes original, boolean classValuesAsString) {
        AnnotationAttributes result = new AnnotationAttributes(original);
        AnnotationUtils.postProcessAnnotationAttributes(annotatedElement, result, classValuesAsString);
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            try {
                Object value = entry.getValue();
                if (value instanceof AnnotationAttributes) {
                    value = convertClassValues(annotatedElement, classLoader, (AnnotationAttributes) value, classValuesAsString);
                } else if (value instanceof AnnotationAttributes[]) {
                    AnnotationAttributes[] values = (AnnotationAttributes[]) value;
                    for (int i = 0; i < values.length; i++) {
                        values[i] = convertClassValues(annotatedElement, classLoader, values[i], classValuesAsString);
                    }
                    value = values;
                } else if (value instanceof Type) {
                    value = (classValuesAsString ? ((Type) value).getClassName() : ClassUtils.forName(((Type) value).getClassName(), classLoader));
                } else if (value instanceof Type[]) {
                    Type[] array = (Type[]) value;
                    Object[] convArray = (classValuesAsString ? new String[array.length] : new Class<?>[array.length]);
                    for (int i = 0; i < array.length; i++) {
                        convArray[i] = (classValuesAsString ? array[i].getClassName() : ClassUtils.forName(array[i].getClassName(), classLoader));
                    }
                    value = convArray;
                } else if (classValuesAsString) {
                    if (value instanceof Class) {
                        value = ((Class<?>) value).getName();
                    } else if (value instanceof Class[]) {
                        Class<?>[] clazzArray = (Class<?>[]) value;
                        String[] newValue = new String[clazzArray.length];
                        for (int i = 0; i < clazzArray.length; i++) {
                            newValue[i] = clazzArray[i].getName();
                        }
                        value = newValue;
                    }
                }
                entry.setValue(value);
            } catch (Throwable ex) {
                // Class not found - can't resolve class reference in annotation attribute.
                result.put(entry.getKey(), ex);
            }
        }
        return result;
    }

    @Nullable
    public static AnnotationAttributes getMergedAnnotationAttributes(LinkedMultiValueMap<String, AnnotationAttributes> attributesMap, Map<String, Set<String>> metaAnnotationMap, String annotationName) {
        // Get the unmerged list of attributes for the target annotation.
        List<AnnotationAttributes> attributesList = attributesMap.get(annotationName);
        if (CollectionUtils.isEmpty(attributesList)) {
            return null;
        }
        // To start with, we populate the result with a copy of all attribute values
        // from the target annotation. A copy is necessary so that we do not
        // inadvertently mutate the state of the metadata passed to this method.
        AnnotationAttributes result = new AnnotationAttributes(attributesList.get(0));
        Set<String> overridableAttributeNames = new HashSet<>(result.keySet());
        overridableAttributeNames.remove(AnnotationUtils.VALUE);
        // Since the map is a LinkedMultiValueMap, we depend on the ordering of
        // elements in the map and reverse the order of the keys in order to traverse
        // "down" the annotation hierarchy.
        List<String> annotationTypes = new ArrayList<>(attributesMap.keySet());
        Collections.reverse(annotationTypes);
        // No need to revisit the target annotation type:
        annotationTypes.remove(annotationName);
        for (String currentAnnotationType : annotationTypes) {
            List<AnnotationAttributes> currentAttributesList = attributesMap.get(currentAnnotationType);
            if (!ObjectUtils.isEmpty(currentAttributesList)) {
                Set<String> metaAnns = metaAnnotationMap.get(currentAnnotationType);
                if (metaAnns != null && metaAnns.contains(annotationName)) {
                    AnnotationAttributes currentAttributes = currentAttributesList.get(0);
                    for (String overridableAttributeName : overridableAttributeNames) {
                        Object value = currentAttributes.get(overridableAttributeName);
                        if (value != null) {
                            // Store the value, potentially overriding a value from an attribute
                            // of the same name found higher in the annotation hierarchy.
                            result.put(overridableAttributeName, value);
                        }
                    }
                }
            }
        }
        return result;
    }

}
