package org.springframework.validation.beanvalidation;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.util.Assert;

import javax.validation.MessageInterpolator;
import java.util.Locale;

public class LocaleContextMessageInterpolator implements MessageInterpolator {

    private final MessageInterpolator targetInterpolator;

    public LocaleContextMessageInterpolator(MessageInterpolator targetInterpolator) {
        Assert.notNull(targetInterpolator, "Target MessageInterpolator must not be null");
        this.targetInterpolator = targetInterpolator;
    }

    @Override
    public String interpolate(String message, Context context) {
        return this.targetInterpolator.interpolate(message, context, LocaleContextHolder.getLocale());
    }

    @Override
    public String interpolate(String message, Context context, Locale locale) {
        return this.targetInterpolator.interpolate(message, context, locale);
    }

}
