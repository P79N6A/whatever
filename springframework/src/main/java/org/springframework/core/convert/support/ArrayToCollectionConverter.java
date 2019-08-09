package org.springframework.core.convert.support;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

final class ArrayToCollectionConverter implements ConditionalGenericConverter {

    private final ConversionService conversionService;

    public ArrayToCollectionConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Object[].class, Collection.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return ConversionUtils.canConvertElements(sourceType.getElementTypeDescriptor(), targetType.getElementTypeDescriptor(), this.conversionService);
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        int length = Array.getLength(source);
        TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
        Collection<Object> target = CollectionFactory.createCollection(targetType.getType(), (elementDesc != null ? elementDesc.getType() : null), length);
        if (elementDesc == null) {
            for (int i = 0; i < length; i++) {
                Object sourceElement = Array.get(source, i);
                target.add(sourceElement);
            }
        } else {
            for (int i = 0; i < length; i++) {
                Object sourceElement = Array.get(source, i);
                Object targetElement = this.conversionService.convert(sourceElement, sourceType.elementTypeDescriptor(sourceElement), elementDesc);
                target.add(targetElement);
            }
        }
        return target;
    }

}
