package org.springframework.web.server.i18n;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

import java.util.Locale;
import java.util.TimeZone;

public class FixedLocaleContextResolver implements LocaleContextResolver {

    private final Locale locale;

    @Nullable
    private final TimeZone timeZone;

    public FixedLocaleContextResolver() {
        this(Locale.getDefault());
    }

    public FixedLocaleContextResolver(Locale locale) {
        this(locale, null);
    }

    public FixedLocaleContextResolver(Locale locale, @Nullable TimeZone timeZone) {
        Assert.notNull(locale, "Locale must not be null");
        this.locale = locale;
        this.timeZone = timeZone;
    }

    @Override
    public LocaleContext resolveLocaleContext(ServerWebExchange exchange) {
        return new TimeZoneAwareLocaleContext() {
            @Override
            public Locale getLocale() {
                return locale;
            }

            @Override
            @Nullable
            public TimeZone getTimeZone() {
                return timeZone;
            }
        };
    }

    @Override
    public void setLocaleContext(ServerWebExchange exchange, @Nullable LocaleContext localeContext) {
        throw new UnsupportedOperationException("Cannot change fixed locale - use a different locale context resolution strategy");
    }

}
