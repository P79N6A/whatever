package org.springframework.format.datetime.standard;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.Month;
import java.util.Locale;

class MonthFormatter implements Formatter<Month> {

    @Override
    public Month parse(String text, Locale locale) throws ParseException {
        return Month.valueOf(text.toUpperCase());
    }

    @Override
    public String print(Month object, Locale locale) {
        return object.toString();
    }

}
