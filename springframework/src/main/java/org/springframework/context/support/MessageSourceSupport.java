package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public abstract class MessageSourceSupport {

    private static final MessageFormat INVALID_MESSAGE_FORMAT = new MessageFormat("");

    protected final Log logger = LogFactory.getLog(getClass());

    private boolean alwaysUseMessageFormat = false;

    private final Map<String, Map<Locale, MessageFormat>> messageFormatsPerMessage = new HashMap<>();

    public void setAlwaysUseMessageFormat(boolean alwaysUseMessageFormat) {
        this.alwaysUseMessageFormat = alwaysUseMessageFormat;
    }

    protected boolean isAlwaysUseMessageFormat() {
        return this.alwaysUseMessageFormat;
    }

    protected String renderDefaultMessage(String defaultMessage, @Nullable Object[] args, Locale locale) {
        return formatMessage(defaultMessage, args, locale);
    }

    protected String formatMessage(String msg, @Nullable Object[] args, Locale locale) {
        if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
            return msg;
        }
        MessageFormat messageFormat = null;
        synchronized (this.messageFormatsPerMessage) {
            Map<Locale, MessageFormat> messageFormatsPerLocale = this.messageFormatsPerMessage.get(msg);
            if (messageFormatsPerLocale != null) {
                messageFormat = messageFormatsPerLocale.get(locale);
            } else {
                messageFormatsPerLocale = new HashMap<>();
                this.messageFormatsPerMessage.put(msg, messageFormatsPerLocale);
            }
            if (messageFormat == null) {
                try {
                    messageFormat = createMessageFormat(msg, locale);
                } catch (IllegalArgumentException ex) {
                    // Invalid message format - probably not intended for formatting,
                    // rather using a message structure with no arguments involved...
                    if (isAlwaysUseMessageFormat()) {
                        throw ex;
                    }
                    // Silently proceed with raw message if format not enforced...
                    messageFormat = INVALID_MESSAGE_FORMAT;
                }
                messageFormatsPerLocale.put(locale, messageFormat);
            }
        }
        if (messageFormat == INVALID_MESSAGE_FORMAT) {
            return msg;
        }
        synchronized (messageFormat) {
            return messageFormat.format(resolveArguments(args, locale));
        }
    }

    protected MessageFormat createMessageFormat(String msg, Locale locale) {
        return new MessageFormat(msg, locale);
    }

    protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
        return (args != null ? args : new Object[0]);
    }

}
