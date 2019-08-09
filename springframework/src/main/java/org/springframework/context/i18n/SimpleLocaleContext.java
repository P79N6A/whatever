package org.springframework.context.i18n;

import org.springframework.lang.Nullable;

import java.util.Locale;

public class SimpleLocaleContext implements LocaleContext {

    @Nullable
    private final Locale locale;

    public SimpleLocaleContext(@Nullable Locale locale) {
        this.locale = locale;
    }

    @Override
    @Nullable
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public String toString() {
        return (this.locale != null ? this.locale.toString() : "-");
    }

}
