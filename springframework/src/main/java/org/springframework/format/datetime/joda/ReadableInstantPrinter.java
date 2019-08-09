package org.springframework.format.datetime.joda;

import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.Printer;

import java.util.Locale;

public final class ReadableInstantPrinter implements Printer<ReadableInstant> {

    private final DateTimeFormatter formatter;

    public ReadableInstantPrinter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public String print(ReadableInstant instant, Locale locale) {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
    }

}
