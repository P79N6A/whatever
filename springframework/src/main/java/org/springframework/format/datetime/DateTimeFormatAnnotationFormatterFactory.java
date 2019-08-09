package org.springframework.format.datetime;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Formatter;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.util.*;

public class DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport implements AnnotationFormatterFactory<DateTimeFormat> {

    private static final Set<Class<?>> FIELD_TYPES;

    static {
        Set<Class<?>> fieldTypes = new HashSet<>(4);
        fieldTypes.add(Date.class);
        fieldTypes.add(Calendar.class);
        fieldTypes.add(Long.class);
        FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
    }

    @Override
    public Set<Class<?>> getFieldTypes() {
        return FIELD_TYPES;
    }

    @Override
    public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
        return getFormatter(annotation, fieldType);
    }

    @Override
    public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
        return getFormatter(annotation, fieldType);
    }

    protected Formatter<Date> getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
        DateFormatter formatter = new DateFormatter();
        String style = resolveEmbeddedValue(annotation.style());
        if (StringUtils.hasLength(style)) {
            formatter.setStylePattern(style);
        }
        formatter.setIso(annotation.iso());
        String pattern = resolveEmbeddedValue(annotation.pattern());
        if (StringUtils.hasLength(pattern)) {
            formatter.setPattern(pattern);
        }
        return formatter;
    }

}
