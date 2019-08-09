package org.springframework.web.servlet.tags.form;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class ErrorsTag extends AbstractHtmlElementBodyTag implements BodyTag {

    public static final String MESSAGES_ATTRIBUTE = "messages";

    public static final String SPAN_TAG = "span";

    private String element = SPAN_TAG;

    private String delimiter = "<br/>";

    @Nullable
    private Object oldMessages;

    private boolean errorMessagesWereExposed;

    public void setElement(String element) {
        Assert.hasText(element, "'element' cannot be null or blank");
        this.element = element;
    }

    public String getElement() {
        return this.element;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getDelimiter() {
        return this.delimiter;
    }

    @Override
    protected String autogenerateId() throws JspException {
        String path = getPropertyPath();
        if ("".equals(path) || "*".equals(path)) {
            path = (String) this.pageContext.getAttribute(FormTag.MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        }
        return StringUtils.deleteAny(path, "[]") + ".errors";
    }

    @Override
    @Nullable
    protected String getName() throws JspException {
        return null;
    }

    @Override
    protected boolean shouldRender() throws JspException {
        try {
            return getBindStatus().isError();
        } catch (IllegalStateException ex) {
            // Neither BindingResult nor target object available.
            return false;
        }
    }

    @Override
    protected void renderDefaultContent(TagWriter tagWriter) throws JspException {
        tagWriter.startTag(getElement());
        writeDefaultAttributes(tagWriter);
        String delimiter = ObjectUtils.getDisplayString(evaluate("delimiter", getDelimiter()));
        String[] errorMessages = getBindStatus().getErrorMessages();
        for (int i = 0; i < errorMessages.length; i++) {
            String errorMessage = errorMessages[i];
            if (i > 0) {
                tagWriter.appendValue(delimiter);
            }
            tagWriter.appendValue(getDisplayString(errorMessage));
        }
        tagWriter.endTag();
    }

    @Override
    protected void exposeAttributes() throws JspException {
        List<String> errorMessages = new ArrayList<>(Arrays.asList(getBindStatus().getErrorMessages()));
        this.oldMessages = this.pageContext.getAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
        this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, errorMessages, PageContext.PAGE_SCOPE);
        this.errorMessagesWereExposed = true;
    }

    @Override
    protected void removeAttributes() {
        if (this.errorMessagesWereExposed) {
            if (this.oldMessages != null) {
                this.pageContext.setAttribute(MESSAGES_ATTRIBUTE, this.oldMessages, PageContext.PAGE_SCOPE);
                this.oldMessages = null;
            } else {
                this.pageContext.removeAttribute(MESSAGES_ATTRIBUTE, PageContext.PAGE_SCOPE);
            }
        }
    }

}
