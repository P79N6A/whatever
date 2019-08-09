package org.springframework.web.servlet.i18n;

import org.springframework.lang.Nullable;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

public abstract class AbstractLocaleResolver implements LocaleResolver {

    @Nullable
    private Locale defaultLocale;

    public void setDefaultLocale(@Nullable Locale defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    @Nullable
    protected Locale getDefaultLocale() {
        return this.defaultLocale;
    }

}
