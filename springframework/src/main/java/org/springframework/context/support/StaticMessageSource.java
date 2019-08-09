package org.springframework.context.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StaticMessageSource extends AbstractMessageSource {

    private final Map<String, String> messages = new HashMap<>();

    private final Map<String, MessageFormat> cachedMessageFormats = new HashMap<>();

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        return this.messages.get(code + '_' + locale.toString());
    }

    @Override
    @Nullable
    protected MessageFormat resolveCode(String code, Locale locale) {
        String key = code + '_' + locale.toString();
        String msg = this.messages.get(key);
        if (msg == null) {
            return null;
        }
        synchronized (this.cachedMessageFormats) {
            MessageFormat messageFormat = this.cachedMessageFormats.get(key);
            if (messageFormat == null) {
                messageFormat = createMessageFormat(msg, locale);
                this.cachedMessageFormats.put(key, messageFormat);
            }
            return messageFormat;
        }
    }

    public void addMessage(String code, Locale locale, String msg) {
        Assert.notNull(code, "Code must not be null");
        Assert.notNull(locale, "Locale must not be null");
        Assert.notNull(msg, "Message must not be null");
        this.messages.put(code + '_' + locale.toString(), msg);
        if (logger.isDebugEnabled()) {
            logger.debug("Added message [" + msg + "] for code [" + code + "] and Locale [" + locale + "]");
        }
    }

    public void addMessages(Map<String, String> messages, Locale locale) {
        Assert.notNull(messages, "Messages Map must not be null");
        messages.forEach((code, msg) -> addMessage(code, locale, msg));
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + this.messages;
    }

}
