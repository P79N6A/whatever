package org.springframework.format.datetime.standard;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.Duration;
import java.util.Locale;

class DurationFormatter implements Formatter<Duration> {

    @Override
    public Duration parse(String text, Locale locale) throws ParseException {
        return Duration.parse(text);
    }

    @Override
    public String print(Duration object, Locale locale) {
        return object.toString();
    }

}
