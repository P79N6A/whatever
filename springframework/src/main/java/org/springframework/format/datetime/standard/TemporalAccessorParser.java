package org.springframework.format.datetime.standard;

import org.springframework.format.Parser;

import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public final class TemporalAccessorParser implements Parser<TemporalAccessor> {

    private final Class<? extends TemporalAccessor> temporalAccessorType;

    private final DateTimeFormatter formatter;

    public TemporalAccessorParser(Class<? extends TemporalAccessor> temporalAccessorType, DateTimeFormatter formatter) {
        this.temporalAccessorType = temporalAccessorType;
        this.formatter = formatter;
    }

    @Override
    public TemporalAccessor parse(String text, Locale locale) throws ParseException {
        DateTimeFormatter formatterToUse = DateTimeContextHolder.getFormatter(this.formatter, locale);
        if (LocalDate.class == this.temporalAccessorType) {
            return LocalDate.parse(text, formatterToUse);
        } else if (LocalTime.class == this.temporalAccessorType) {
            return LocalTime.parse(text, formatterToUse);
        } else if (LocalDateTime.class == this.temporalAccessorType) {
            return LocalDateTime.parse(text, formatterToUse);
        } else if (ZonedDateTime.class == this.temporalAccessorType) {
            return ZonedDateTime.parse(text, formatterToUse);
        } else if (OffsetDateTime.class == this.temporalAccessorType) {
            return OffsetDateTime.parse(text, formatterToUse);
        } else if (OffsetTime.class == this.temporalAccessorType) {
            return OffsetTime.parse(text, formatterToUse);
        } else {
            throw new IllegalStateException("Unsupported TemporalAccessor type: " + this.temporalAccessorType);
        }
    }

}
