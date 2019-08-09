package org.springframework.format.datetime.standard;

import org.springframework.format.FormatterRegistrar;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.EnumMap;
import java.util.Map;

public class DateTimeFormatterRegistrar implements FormatterRegistrar {

    private enum Type {DATE, TIME, DATE_TIME}

    private final Map<Type, DateTimeFormatter> formatters = new EnumMap<>(Type.class);

    private final Map<Type, DateTimeFormatterFactory> factories = new EnumMap<>(Type.class);

    public DateTimeFormatterRegistrar() {
        for (Type type : Type.values()) {
            this.factories.put(type, new DateTimeFormatterFactory());
        }
    }

    public void setUseIsoFormat(boolean useIsoFormat) {
        this.factories.get(Type.DATE).setIso(useIsoFormat ? ISO.DATE : ISO.NONE);
        this.factories.get(Type.TIME).setIso(useIsoFormat ? ISO.TIME : ISO.NONE);
        this.factories.get(Type.DATE_TIME).setIso(useIsoFormat ? ISO.DATE_TIME : ISO.NONE);
    }

    public void setDateStyle(FormatStyle dateStyle) {
        this.factories.get(Type.DATE).setDateStyle(dateStyle);
    }

    public void setTimeStyle(FormatStyle timeStyle) {
        this.factories.get(Type.TIME).setTimeStyle(timeStyle);
    }

    public void setDateTimeStyle(FormatStyle dateTimeStyle) {
        this.factories.get(Type.DATE_TIME).setDateTimeStyle(dateTimeStyle);
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
        DateTimeConverters.registerConverters(registry);
        DateTimeFormatter df = getFormatter(Type.DATE);
        DateTimeFormatter tf = getFormatter(Type.TIME);
        DateTimeFormatter dtf = getFormatter(Type.DATE_TIME);
        // Efficient ISO_LOCAL_* variants for printing since they are twice as fast...
        registry.addFormatterForFieldType(LocalDate.class, new TemporalAccessorPrinter(df == DateTimeFormatter.ISO_DATE ? DateTimeFormatter.ISO_LOCAL_DATE : df), new TemporalAccessorParser(LocalDate.class, df));
        registry.addFormatterForFieldType(LocalTime.class, new TemporalAccessorPrinter(tf == DateTimeFormatter.ISO_TIME ? DateTimeFormatter.ISO_LOCAL_TIME : tf), new TemporalAccessorParser(LocalTime.class, tf));
        registry.addFormatterForFieldType(LocalDateTime.class, new TemporalAccessorPrinter(dtf == DateTimeFormatter.ISO_DATE_TIME ? DateTimeFormatter.ISO_LOCAL_DATE_TIME : dtf), new TemporalAccessorParser(LocalDateTime.class, dtf));
        registry.addFormatterForFieldType(ZonedDateTime.class, new TemporalAccessorPrinter(dtf), new TemporalAccessorParser(ZonedDateTime.class, dtf));
        registry.addFormatterForFieldType(OffsetDateTime.class, new TemporalAccessorPrinter(dtf), new TemporalAccessorParser(OffsetDateTime.class, dtf));
        registry.addFormatterForFieldType(OffsetTime.class, new TemporalAccessorPrinter(tf), new TemporalAccessorParser(OffsetTime.class, tf));
        registry.addFormatterForFieldType(Instant.class, new InstantFormatter());
        registry.addFormatterForFieldType(Period.class, new PeriodFormatter());
        registry.addFormatterForFieldType(Duration.class, new DurationFormatter());
        registry.addFormatterForFieldType(Year.class, new YearFormatter());
        registry.addFormatterForFieldType(Month.class, new MonthFormatter());
        registry.addFormatterForFieldType(YearMonth.class, new YearMonthFormatter());
        registry.addFormatterForFieldType(MonthDay.class, new MonthDayFormatter());
        registry.addFormatterForFieldAnnotation(new Jsr310DateTimeFormatAnnotationFormatterFactory());
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
                return DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
            case TIME:
                return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
            default:
                return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        }
    }

}
