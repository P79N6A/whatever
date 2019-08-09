package org.springframework.web.servlet.tags;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class MessageTag extends HtmlEscapingAwareTag implements ArgumentAware {

    public static final String DEFAULT_ARGUMENT_SEPARATOR = ",";

    @Nullable
    private MessageSourceResolvable message;

    @Nullable
    private String code;

    @Nullable
    private Object arguments;

    private String argumentSeparator = DEFAULT_ARGUMENT_SEPARATOR;

    private List<Object> nestedArguments = Collections.emptyList();

    @Nullable
    private String text;

    @Nullable
    private String var;

    private String scope = TagUtils.SCOPE_PAGE;

    private boolean javaScriptEscape = false;

    public void setMessage(MessageSourceResolvable message) {
        this.message = message;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setArguments(Object arguments) {
        this.arguments = arguments;
    }

    public void setArgumentSeparator(String argumentSeparator) {
        this.argumentSeparator = argumentSeparator;
    }

    @Override
    public void addArgument(@Nullable Object argument) throws JspTagException {
        this.nestedArguments.add(argument);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
        this.javaScriptEscape = javaScriptEscape;
    }

    @Override
    protected final int doStartTagInternal() throws JspException, IOException {
        this.nestedArguments = new LinkedList<>();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            // Resolve the unescaped message.
            String msg = resolveMessage();
            // HTML and/or JavaScript escape, if demanded.
            msg = htmlEscape(msg);
            msg = this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(msg) : msg;
            // Expose as variable, if demanded, else write to the page.
            if (this.var != null) {
                this.pageContext.setAttribute(this.var, msg, TagUtils.getScope(this.scope));
            } else {
                writeMessage(msg);
            }
            return EVAL_PAGE;
        } catch (IOException ex) {
            throw new JspTagException(ex.getMessage(), ex);
        } catch (NoSuchMessageException ex) {
            throw new JspTagException(getNoSuchMessageExceptionDescription(ex));
        }
    }

    @Override
    public void release() {
        super.release();
        this.arguments = null;
    }

    protected String resolveMessage() throws JspException, NoSuchMessageException {
        MessageSource messageSource = getMessageSource();
        // Evaluate the specified MessageSourceResolvable, if any.
        if (this.message != null) {
            // We have a given MessageSourceResolvable.
            return messageSource.getMessage(this.message, getRequestContext().getLocale());
        }
        if (this.code != null || this.text != null) {
            // We have a code or default text that we need to resolve.
            Object[] argumentsArray = resolveArguments(this.arguments);
            if (!this.nestedArguments.isEmpty()) {
                argumentsArray = appendArguments(argumentsArray, this.nestedArguments.toArray());
            }
            if (this.text != null) {
                // We have a fallback text to consider.
                String msg = messageSource.getMessage(this.code, argumentsArray, this.text, getRequestContext().getLocale());
                return (msg != null ? msg : "");
            } else {
                // We have no fallback text to consider.
                return messageSource.getMessage(this.code, argumentsArray, getRequestContext().getLocale());
            }
        }
        throw new JspTagException("No resolvable message");
    }

    private Object[] appendArguments(@Nullable Object[] sourceArguments, Object[] additionalArguments) {
        if (ObjectUtils.isEmpty(sourceArguments)) {
            return additionalArguments;
        }
        Object[] arguments = new Object[sourceArguments.length + additionalArguments.length];
        System.arraycopy(sourceArguments, 0, arguments, 0, sourceArguments.length);
        System.arraycopy(additionalArguments, 0, arguments, sourceArguments.length, additionalArguments.length);
        return arguments;
    }

    @Nullable
    protected Object[] resolveArguments(@Nullable Object arguments) throws JspException {
        if (arguments instanceof String) {
            String[] stringArray = StringUtils.delimitedListToStringArray((String) arguments, this.argumentSeparator);
            if (stringArray.length == 1) {
                Object argument = stringArray[0];
                if (argument != null && argument.getClass().isArray()) {
                    return ObjectUtils.toObjectArray(argument);
                } else {
                    return new Object[]{argument};
                }
            } else {
                return stringArray;
            }
        } else if (arguments instanceof Object[]) {
            return (Object[]) arguments;
        } else if (arguments instanceof Collection) {
            return ((Collection<?>) arguments).toArray();
        } else if (arguments != null) {
            // Assume a single argument object.
            return new Object[]{arguments};
        } else {
            return null;
        }
    }

    protected void writeMessage(String msg) throws IOException {
        this.pageContext.getOut().write(String.valueOf(msg));
    }

    protected MessageSource getMessageSource() {
        return getRequestContext().getMessageSource();
    }

    protected String getNoSuchMessageExceptionDescription(NoSuchMessageException ex) {
        return ex.getMessage();
    }

}
