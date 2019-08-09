package org.springframework.format.datetime.joda;

import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.Parser;

import java.text.ParseException;
import java.util.Locale;

public final class LocalDateTimeParser implements Parser<LocalDateTime> {

    private final DateTimeFormatter formatter;

    public LocalDateTimeParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public LocalDateTime parse(String text, Locale locale) throws ParseException {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalDateTime(text);
    }

}
