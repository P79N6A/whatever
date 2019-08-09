package org.springframework.context.i18n;

import org.springframework.lang.Nullable;

import java.util.TimeZone;

public interface TimeZoneAwareLocaleContext extends LocaleContext {

    @Nullable
    TimeZone getTimeZone();

}
