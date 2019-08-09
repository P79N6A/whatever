package org.springframework.format.datetime.joda;

import org.joda.time.*;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.support.EmbeddedValueResolutionSupport;
import org.springframework.format.AnnotationFormatterFactory;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;

import java.util.*;

public class JodaDateTimeFormatAnnotationFormatterFactory extends EmbeddedValueResolutionSupport implements AnnotationFormatterFactory<DateTimeFormat> {

    private static final Set<Class<?>> FIELD_TYPES;

    static {
        // Create the set of field types that may be annotated with @DateTimeFormat.
        // Note: the 3 ReadablePartial concrete types are registered explicitly since
        // addFormatterForFieldType rules exist for each of these types
        // (if we did not do this, the default byType rules for LocalDate, LocalTime,
        // and LocalDateTime would take precedence over the annotation rule, which
        // is not what we want)
        Set<Class<?>> fieldTypes = new HashSet<>(8);
        fieldTypes.add(ReadableInstant.class);
        fieldTypes.add(LocalDate.class);
        fieldTypes.add(LocalTime.class);
        fieldTypes.add(LocalDateTime.class);
        fieldTypes.add(Date.class);
        fieldTypes.add(Calendar.class);
        fieldTypes.add(Long.class);
        FIELD_TYPES = Collections.unmodifiableSet(fieldTypes);
    }

    @Override
    public final Set<Class<?>> getFieldTypes() {
        return FIELD_TYPES;
    }

    @Override
    public Printer<?> getPrinter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatter formatter = getFormatter(annotation, fieldType);
        if (ReadablePartial.class.isAssignableFrom(fieldType)) {
            return new ReadablePartialPrinter(formatter);
        } else if (ReadableInstant.class.isAssignableFrom(fieldType) || Calendar.class.isAssignableFrom(fieldType)) {
            // assumes Calendar->ReadableInstant converter is registered
            return new ReadableInstantPrinter(formatter);
        } else {
            // assumes Date->Long converter is registered
            return new MillisecondInstantPrinter(formatter);
        }
    }

    @Override
    public Parser<?> getParser(DateTimeFormat annotation, Class<?> fieldType) {
        if (LocalDate.class == fieldType) {
            return new LocalDateParser(getFormatter(annotation, fieldType));
        } else if (LocalTime.class == fieldType) {
            return new LocalTimeParser(getFormatter(annotation, fieldType));
        } else if (LocalDateTime.class == fieldType) {
            return new LocalDateTimeParser(getFormatter(annotation, fieldType));
        } else {
            return new DateTimeParser(getFormatter(annotation, fieldType));
        }
    }

    protected DateTimeFormatter getFormatter(DateTimeFormat annotation, Class<?> fieldType) {
        DateTimeFormatterFactory factory = new DateTimeFormatterFactory();
        String style = resolveEmbeddedValue(annotation.style());
        if (StringUtils.hasLength(style)) {
            factory.setStyle(style);
        }
        factory.setIso(annotation.iso());
        String pattern = resolveEmbeddedValue(annotation.pattern());
        if (StringUtils.hasLength(pattern)) {
            factory.setPattern(pattern);
        }
        return factory.createDateTimeFormatter();
    }

}
