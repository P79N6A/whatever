package org.springframework.format.datetime.standard;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.Year;
import java.util.Locale;

class YearFormatter implements Formatter<Year> {

    @Override
    public Year parse(String text, Locale locale) throws ParseException {
        return Year.parse(text);
    }

    @Override
    public String print(Year object, Locale locale) {
        return object.toString();
    }

}
