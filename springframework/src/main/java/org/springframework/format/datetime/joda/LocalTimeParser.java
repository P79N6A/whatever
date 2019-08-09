package org.springframework.format.datetime.joda;

import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.Parser;

import java.text.ParseException;
import java.util.Locale;

public final class LocalTimeParser implements Parser<LocalTime> {

    private final DateTimeFormatter formatter;

    public LocalTimeParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public LocalTime parse(String text, Locale locale) throws ParseException {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseLocalTime(text);
    }

}
