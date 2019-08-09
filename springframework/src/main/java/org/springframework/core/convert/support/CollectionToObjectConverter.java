package org.springframework.core.convert.support;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

final class CollectionToObjectConverter implements ConditionalGenericConverter {

    private final ConversionService conversionService;

    public CollectionToObjectConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Collection.class, Object.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(), targetType, this.conversionService);
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        if (sourceType.isAssignableTo(targetType)) {
            return source;
        }
        Collection<?> sourceCollection = (Collection<?>) source;
        if (sourceCollection.isEmpty()) {
            return null;
        }
        Object firstElement = sourceCollection.iterator().next();
        return this.conversionService.convert(firstElement, sourceType.elementTypeDescriptor(firstElement), targetType);
    }

}
