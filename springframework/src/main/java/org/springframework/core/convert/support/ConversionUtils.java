package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

abstract class ConversionUtils {

    @Nullable
    public static Object invokeConverter(GenericConverter converter, @Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        try {
            return converter.convert(source, sourceType, targetType);
        } catch (ConversionFailedException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new ConversionFailedException(sourceType, targetType, source, ex);
        }
    }

    public static boolean canConvertElements(@Nullable TypeDescriptor sourceElementType, @Nullable TypeDescriptor targetElementType, ConversionService conversionService) {
        if (targetElementType == null) {
            // yes
            return true;
        }
        if (sourceElementType == null) {
            // maybe
            return true;
        }
        if (conversionService.canConvert(sourceElementType, targetElementType)) {
            // yes
            return true;
        }
        if (ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType())) {
            // maybe
            return true;
        }
        // no
        return false;
    }

    public static Class<?> getEnumType(Class<?> targetType) {
        Class<?> enumType = targetType;
        while (enumType != null && !enumType.isEnum()) {
            enumType = enumType.getSuperclass();
        }
        Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
        return enumType;
    }

}
