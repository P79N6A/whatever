package org.springframework.context.i18n;

import org.springframework.lang.Nullable;

import java.util.Locale;

public interface LocaleContext {

    @Nullable
    Locale getLocale();

}
