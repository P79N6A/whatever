package org.springframework.format.datetime.joda;

import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.Printer;

import java.util.Locale;

public final class MillisecondInstantPrinter implements Printer<Long> {

    private final DateTimeFormatter formatter;

    public MillisecondInstantPrinter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public String print(Long instant, Locale locale) {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(instant);
    }

}
