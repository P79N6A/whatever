package org.springframework.validation.beanvalidation;

import org.hibernate.validator.spi.resourceloading.ResourceBundleLocator;
import org.springframework.context.MessageSource;
import org.springframework.context.support.MessageSourceResourceBundle;
import org.springframework.util.Assert;

import java.util.Locale;
import java.util.ResourceBundle;

public class MessageSourceResourceBundleLocator implements ResourceBundleLocator {

    private final MessageSource messageSource;

    public MessageSourceResourceBundleLocator(MessageSource messageSource) {
        Assert.notNull(messageSource, "MessageSource must not be null");
        this.messageSource = messageSource;
    }

    @Override
    public ResourceBundle getResourceBundle(Locale locale) {
        return new MessageSourceResourceBundle(this.messageSource, locale);
    }

}
