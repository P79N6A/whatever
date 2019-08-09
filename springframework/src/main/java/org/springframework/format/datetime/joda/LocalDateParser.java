package org.springframework.format.datetime.joda;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.Parser;

import java.text.ParseException;
import java.util.Locale;

public final class LocalDateParser implements Parser<LocalDate> {

    private final DateTimeFormatter formatter;

    public LocalDateParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public LocalDate parse(String text, Locale locale) throws ParseException {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalDate(text);
    }

}
