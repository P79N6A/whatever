package org.springframework.format.datetime.standard;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.YearMonth;
import java.util.Locale;

class YearMonthFormatter implements Formatter<YearMonth> {

    @Override
    public YearMonth parse(String text, Locale locale) throws ParseException {
        return YearMonth.parse(text);
    }

    @Override
    public String print(YearMonth object, Locale locale) {
        return object.toString();
    }

}
