package org.springframework.web.servlet.tags;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

@SuppressWarnings("serial")
public class ThemeTag extends MessageTag {

    @Override
    protected MessageSource getMessageSource() {
        return getRequestContext().getTheme().getMessageSource();
    }

    @Override
    protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
        return "Theme '" + getRequestContext().getTheme().getName() + "': " + ex.getMessage();
    }

}
