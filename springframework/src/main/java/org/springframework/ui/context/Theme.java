package org.springframework.ui.context;

import org.springframework.context.MessageSource;

public interface Theme {

    String getName();

    MessageSource getMessageSource();

}
