package org.springframework.format.datetime.joda;

import org.joda.time.MonthDay;
import org.springframework.format.Formatter;

import java.text.ParseException;
import java.util.Locale;

class MonthDayFormatter implements Formatter<MonthDay> {

    @Override
    public MonthDay parse(String text, Locale locale) throws ParseException {
        return MonthDay.parse(text);
    }

    @Override
    public String print(MonthDay object, Locale locale) {
        return object.toString();
    }

}
