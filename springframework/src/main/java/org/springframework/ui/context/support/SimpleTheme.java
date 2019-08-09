package org.springframework.ui.context.support;

import org.springframework.context.MessageSource;
import org.springframework.ui.context.Theme;
import org.springframework.util.Assert;

public class SimpleTheme implements Theme {

    private final String name;

    private final MessageSource messageSource;

    public SimpleTheme(String name, MessageSource messageSource) {
        Assert.notNull(name, "Name must not be null");
        Assert.notNull(messageSource, "MessageSource must not be null");
        this.name = name;
        this.messageSource = messageSource;
    }

    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final MessageSource getMessageSource() {
        return this.messageSource;
    }

}
