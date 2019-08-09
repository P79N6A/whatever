package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

final class ZonedDateTimeToCalendarConverter implements Converter<ZonedDateTime, Calendar> {

    @Override
    public Calendar convert(ZonedDateTime source) {
        return GregorianCalendar.from(source);
    }

}
