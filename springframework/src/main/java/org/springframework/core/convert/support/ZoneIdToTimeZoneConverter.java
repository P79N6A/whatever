package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

import java.time.ZoneId;
import java.util.TimeZone;

final class ZoneIdToTimeZoneConverter implements Converter<ZoneId, TimeZone> {

    @Override
    public TimeZone convert(ZoneId source) {
        return TimeZone.getTimeZone(source);
    }

}
