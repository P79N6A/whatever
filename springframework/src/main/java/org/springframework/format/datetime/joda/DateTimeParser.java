package org.springframework.format.datetime.joda;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.Parser;

import java.text.ParseException;
import java.util.Locale;

public final class DateTimeParser implements Parser<DateTime> {

    private final DateTimeFormatter formatter;

    public DateTimeParser(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public DateTime parse(String text, Locale locale) throws ParseException {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).parseDateTime(text);
    }

}
