package org.springframework.context;

import org.springframework.lang.Nullable;

import java.util.Locale;

public interface MessageSource {

    @Nullable
    String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

    String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException;

    String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException;

}
