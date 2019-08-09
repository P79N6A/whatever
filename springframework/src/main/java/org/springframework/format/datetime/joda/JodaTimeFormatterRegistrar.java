package org.springframework.format.datetime.joda;

import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.Parser;
import org.springframework.format.Printer;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;

public class JodaTimeFormatterRegistrar implements FormatterRegistrar {

    private enum Type {DATE, TIME, DATE_TIME}

    private final Map<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);

    private final Map<Type, DateTimeFormatterFactory> factories;

    public JodaTimeFormatterRegistrar() {
        this.factories = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            this.factories.put(type, new DateTimeFormatterFactory());
        }
    }

    public void setUseIsoFormat(boolean useIsoFormat) {
        this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
        this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
        this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
    }

    public void setDateStyle(String dateStyle) {
        this.factories.get(Type.DATE).setStyle(dateStyle + "-");
    }

    public void setTimeStyle(String timeStyle) {
        this.factories.get(Type.TIME).setStyle("-" + timeStyle);
    }

    public void setDateTimeStyle(String dateTimeStyle) {
        this.factories.get(Type.DATE_TIME).setStyle(dateTimeStyle);
    }

    public void setDateFormatter(DateTimeFormatter formatter) {
        this.formatters.put(Type.DATE, formatter);
    }

    public void setTimeFormatter(DateTimeFormatter formatter) {
        this.formatters.put(Type.TIME, formatter);
    }

    public void setDateTimeFormatter(DateTimeFormatter formatter) {
        this.formatters.put(Type.DATE_TIME, formatter);
    }

    @Override
    public void registerFormatters(FormatterRegistry registry) {
        JodaTimeConverters.registerConverters(registry);
        DateTimeFormatter dateFormatter = getFormatter(Type.DATE);
        DateTimeFormatter timeFormatter = getFormatter(Type.TIME);
        DateTimeFormatter dateTimeFormatter = getFormatter(Type.DATE_TIME);
        addFormatterForFields(registry, new ReadablePartialPrinter(dateFormatter), new LocalDateParser(dateFormatter), LocalDate.class);
        addFormatterForFields(registry, new ReadablePartialPrinter(timeFormatter), new LocalTimeParser(timeFormatter), LocalTime.class);
        addFormatterForFields(registry, new ReadablePartialPrinter(dateTimeFormatter), new LocalDateTimeParser(dateTimeFormatter), LocalDateTime.class);
        addFormatterForFields(registry, new ReadableInstantPrinter(dateTimeFormatter), new DateTimeParser(dateTimeFormatter), ReadableInstant.class);
        // In order to retain backwards compatibility we only register Date/Calendar
        // types when a user defined formatter is specified (see SPR-10105)
        if (this.formatters.containsKey(Type.DATE_TIME)) {
            addFormatterForFields(registry, new ReadableInstantPrinter(dateTimeFormatter), new DateTimeParser(dateTimeFormatter), Date.class, Calendar.class);
        }
        registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
        registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
        registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
        registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());
        registry.addFormatterForFieldAnnotation(new JodaDateTimeFormatAnnotationFormatterFactory());
    }

    private DateTimeFormatter getFormatter(Type type) {
        DateTimeFormatter formatter = this.formatters.get(type);
        if (formatter != null) {
            return formatter;
        }
        DateTimeFormatter fallbackFormatter = getFallbackFormatter(type);
        return this.factories.get(type).createDateTimeFormatter(fallbackFormatter);
    }

    private DateTimeFormatter getFallbackFormatter(Type type) {
        switch (type) {
            case DATE:
                return DateTimeFormat.shortDate();
            case TIME:
                return DateTimeFormat.shortTime();
            default:
                return DateTimeFormat.shortDateTime();
        }
    }

    private void addFormatterForFields(FormatterRegistry registry, Printer<?> printer, Parser<?> parser, Class<?>... fieldTypes) {
        for (Class<?> fieldType : fieldTypes) {
            registry.addFormatterForFieldType(fieldType, printer, parser);
        }
    }

}
