package org.springframework.context.support;

import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public abstract class AbstractMessageSource extends MessageSourceSupport implements HierarchicalMessageSource {

    @Nullable
    private MessageSource parentMessageSource;

    @Nullable
    private Properties commonMessages;

    private boolean useCodeAsDefaultMessage = false;

    @Override
    public void setParentMessageSource(@Nullable MessageSource parent) {
        this.parentMessageSource = parent;
    }

    @Override
    @Nullable
    public MessageSource getParentMessageSource() {
        return this.parentMessageSource;
    }

    public void setCommonMessages(@Nullable Properties commonMessages) {
        this.commonMessages = commonMessages;
    }

    @Nullable
    protected Properties getCommonMessages() {
        return this.commonMessages;
    }

    public void setUseCodeAsDefaultMessage(boolean useCodeAsDefaultMessage) {
        this.useCodeAsDefaultMessage = useCodeAsDefaultMessage;
    }

    protected boolean isUseCodeAsDefaultMessage() {
        return this.useCodeAsDefaultMessage;
    }

    @Override
    public final String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
        String msg = getMessageInternal(code, args, locale);
        if (msg != null) {
            return msg;
        }
        if (defaultMessage == null) {
            return getDefaultMessage(code);
        }
        return renderDefaultMessage(defaultMessage, args, locale);
    }

    @Override
    public final String getMessage(String code, @Nullable Object[] args, Locale locale) throws NoSuchMessageException {
        String msg = getMessageInternal(code, args, locale);
        if (msg != null) {
            return msg;
        }
        String fallback = getDefaultMessage(code);
        if (fallback != null) {
            return fallback;
        }
        throw new NoSuchMessageException(code, locale);
    }

    @Override
    public final String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        String[] codes = resolvable.getCodes();
        if (codes != null) {
            for (String code : codes) {
                String message = getMessageInternal(code, resolvable.getArguments(), locale);
                if (message != null) {
                    return message;
                }
            }
        }
        String defaultMessage = getDefaultMessage(resolvable, locale);
        if (defaultMessage != null) {
            return defaultMessage;
        }
        throw new NoSuchMessageException(!ObjectUtils.isEmpty(codes) ? codes[codes.length - 1] : "", locale);
    }

    @Nullable
    protected String getMessageInternal(@Nullable String code, @Nullable Object[] args, @Nullable Locale locale) {
        if (code == null) {
            return null;
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        Object[] argsToUse = args;
        if (!isAlwaysUseMessageFormat() && ObjectUtils.isEmpty(args)) {
            // Optimized resolution: no arguments to apply,
            // therefore no MessageFormat needs to be involved.
            // Note that the default implementation still uses MessageFormat;
            // this can be overridden in specific subclasses.
            String message = resolveCodeWithoutArguments(code, locale);
            if (message != null) {
                return message;
            }
        } else {
            // Resolve arguments eagerly, for the case where the message
            // is defined in a parent MessageSource but resolvable arguments
            // are defined in the child MessageSource.
            argsToUse = resolveArguments(args, locale);
            MessageFormat messageFormat = resolveCode(code, locale);
            if (messageFormat != null) {
                synchronized (messageFormat) {
                    return messageFormat.format(argsToUse);
                }
            }
        }
        // Check locale-independent common messages for the given message code.
        Properties commonMessages = getCommonMessages();
        if (commonMessages != null) {
            String commonMessage = commonMessages.getProperty(code);
            if (commonMessage != null) {
                return formatMessage(commonMessage, args, locale);
            }
        }
        // Not found -> check parent, if any.
        return getMessageFromParent(code, argsToUse, locale);
    }

    @Nullable
    protected String getMessageFromParent(String code, @Nullable Object[] args, Locale locale) {
        MessageSource parent = getParentMessageSource();
        if (parent != null) {
            if (parent instanceof AbstractMessageSource) {
                // Call internal method to avoid getting the default code back
                // in case of "useCodeAsDefaultMessage" being activated.
                return ((AbstractMessageSource) parent).getMessageInternal(code, args, locale);
            } else {
                // Check parent MessageSource, returning null if not found there.
                // Covers custom MessageSource impls and DelegatingMessageSource.
                return parent.getMessage(code, args, null, locale);
            }
        }
        // Not found in parent either.
        return null;
    }

    @Nullable
    protected String getDefaultMessage(MessageSourceResolvable resolvable, Locale locale) {
        String defaultMessage = resolvable.getDefaultMessage();
        String[] codes = resolvable.getCodes();
        if (defaultMessage != null) {
            if (resolvable instanceof DefaultMessageSourceResolvable && !((DefaultMessageSourceResolvable) resolvable).shouldRenderDefaultMessage()) {
                // Given default message does not contain any argument placeholders
                // (and isn't escaped for alwaysUseMessageFormat either) -> return as-is.
                return defaultMessage;
            }
            if (!ObjectUtils.isEmpty(codes) && defaultMessage.equals(codes[0])) {
                // Never format a code-as-default-message, even with alwaysUseMessageFormat=true
                return defaultMessage;
            }
            return renderDefaultMessage(defaultMessage, resolvable.getArguments(), locale);
        }
        return (!ObjectUtils.isEmpty(codes) ? getDefaultMessage(codes[0]) : null);
    }

    @Nullable
    protected String getDefaultMessage(String code) {
        if (isUseCodeAsDefaultMessage()) {
            return code;
        }
        return null;
    }

    @Override
    protected Object[] resolveArguments(@Nullable Object[] args, Locale locale) {
        if (ObjectUtils.isEmpty(args)) {
            return super.resolveArguments(args, locale);
        }
        List<Object> resolvedArgs = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (arg instanceof MessageSourceResolvable) {
                resolvedArgs.add(getMessage((MessageSourceResolvable) arg, locale));
            } else {
                resolvedArgs.add(arg);
            }
        }
        return resolvedArgs.toArray();
    }

    @Nullable
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        MessageFormat messageFormat = resolveCode(code, locale);
        if (messageFormat != null) {
            synchronized (messageFormat) {
                return messageFormat.format(new Object[0]);
            }
        }
        return null;
    }

    @Nullable
    protected abstract MessageFormat resolveCode(String code, Locale locale);

}
