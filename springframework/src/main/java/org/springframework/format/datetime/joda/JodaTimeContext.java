package org.springframework.format.datetime.joda;

import org.joda.time.Chronology;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;

import java.util.TimeZone;

public class JodaTimeContext {

    @Nullable
    private Chronology chronology;

    @Nullable
    private DateTimeZone timeZone;

    public void setChronology(@Nullable Chronology chronology) {
        this.chronology = chronology;
    }

    @Nullable
    public Chronology getChronology() {
        return this.chronology;
    }

    public void setTimeZone(@Nullable DateTimeZone timeZone) {
        this.timeZone = timeZone;
    }

    @Nullable
    public DateTimeZone getTimeZone() {
        return this.timeZone;
    }

    public DateTimeFormatter getFormatter(DateTimeFormatter formatter) {
        if (this.chronology != null) {
            formatter = formatter.withChronology(this.chronology);
        }
        if (this.timeZone != null) {
            formatter = formatter.withZone(this.timeZone);
        } else {
            LocaleContext localeContext = LocaleContextHolder.getLocaleContext();
            if (localeContext instanceof TimeZoneAwareLocaleContext) {
                TimeZone timeZone = ((TimeZoneAwareLocaleContext) localeContext).getTimeZone();
                if (timeZone != null) {
                    formatter = formatter.withZone(DateTimeZone.forTimeZone(timeZone));
                }
            }
        }
        return formatter;
    }

}
