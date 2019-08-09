package org.springframework.format.datetime.standard;

import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Jsr310DateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport implements AnnotationFormatterFactory<DateTimeFormat> {

    private static final Set<Class<?>> FIELD_TYPES;

    static {
        // Create the set of field types that may be annotated with @DateTimeFormat.
        Set<Class<?>> fieldTypes = new HashSet<>(8);
        fieldTypes.add(LocalDate.class);
        fieldTypes.add(LocalTime.class);
        fieldTypes.add(LocalDateTime.class);
        fieldTypes.add(ZonedDateTime.class);
        fieldTypes.add(OffsetDateTime.class);
        fieldTypes.add(OffsetTime.class);
        FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
    }

    @Override
    public final Set<Class<?>> getFieldTypes() {
        return FIELD_TYPES;
    }

    @Override
    public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatter formatter = getFormatter(annotation, fieldType);
        // Efficient ISO_LOCAL_* variants for printing since they are twice as fast...
        if (formatter == DateTimeFormatter.ISO_DATE) {
            if (isLocal(fieldType)) {
                formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            }
        } else if (formatter == DateTimeFormatter.ISO_TIME) {
            if (isLocal(fieldType)) {
                formatter = DateTimeFormatter.ISO_LOCAL_TIME;
            }
        } else if (formatter == DateTimeFormatter.ISO_DATE_TIME) {
            if (isLocal(fieldType)) {
                formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            }
        }
        return new TemporalAccessorPrinter(formatter);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatter formatter = getFormatter(annotation, fieldType);
        return new TemporalAccessorParser((Class<? extends TemporalAccessor>) fieldType, formatter);
    }

    protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
        String style = resolveEmbeddedValue(annotation.style());
        if (StringUtils.hasLength(style)) {
            factory.setStylePattern(style);
        }
        factory.setIso(annotation.iso());
        String pattern = resolveEmbeddedValue(annotation.pattern());
        if (StringUtils.hasLength(pattern)) {
            factory.setPattern(pattern);
        }
        return factory.createDateTimeFormatter();
    }

    private boolean isLocal(Class<?> fieldType) {
        return fieldType.getSimpleName().startsWith("Local");
    }

}
