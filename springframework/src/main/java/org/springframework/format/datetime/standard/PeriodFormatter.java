package org.springframework.format.datetime.standard;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.Period;
import java.util.Locale;

class PeriodFormatter implements Formatter<Period> {

    @Override
    public Period parse(String text, Locale locale) throws ParseException {
        return Period.parse(text);
    }

    @Override
    public String print(Period object, Locale locale) {
        return object.toString();
    }

}
