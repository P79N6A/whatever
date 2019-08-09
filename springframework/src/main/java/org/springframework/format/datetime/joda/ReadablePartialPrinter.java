package org.springframework.format.datetime.joda;

import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.Printer;

import java.util.Locale;

public final class ReadablePartialPrinter implements Printer<ReadablePartial> {

    private final DateTimeFormatter formatter;

    public ReadablePartialPrinter(DateTimeFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public String print(ReadablePartial partial, Locale locale) {
        return JodaTimeContextHolder.getFormatter(this.formatter, locale).print(partial);
    }

}
