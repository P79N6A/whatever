package org.springframework.web.servlet.tags.form;

import org.springframework.beans.PropertyAccessor;
import org.springframework.core.Conventions;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Map;

@SuppressWarnings("serial")
public class FormTag extends AbstractHtmlElementTag {

    private static final String DEFAULT_METHOD = "post";

    public static final String DEFAULT_COMMAND_NAME = "command";

    private static final String MODEL_ATTRIBUTE = "modelAttribute";

    public static final String MODEL_ATTRIBUTE_VARIABLE_NAME = Conventions.getQualifiedAttributeName(AbstractFormTag.class, MODEL_ATTRIBUTE);

    private static final String DEFAULT_METHOD_PARAM = "_method";

    private static final String FORM_TAG = "form";

    private static final String INPUT_TAG = "input";

    private static final String ACTION_ATTRIBUTE = "action";

    private static final String METHOD_ATTRIBUTE = "method";

    private static final String TARGET_ATTRIBUTE = "target";

    private static final String ENCTYPE_ATTRIBUTE = "enctype";

    private static final String ACCEPT_CHARSET_ATTRIBUTE = "accept-charset";

    private static final String ONSUBMIT_ATTRIBUTE = "onsubmit";

    private static final String ONRESET_ATTRIBUTE = "onreset";

    private static final String AUTOCOMPLETE_ATTRIBUTE = "autocomplete";

    private static final String NAME_ATTRIBUTE = "name";

    private static final String VALUE_ATTRIBUTE = "value";

    private static final String TYPE_ATTRIBUTE = "type";

    @Nullable
    private TagWriter tagWriter;

    private String modelAttribute = DEFAULT_COMMAND_NAME;

    @Nullable
    private String name;

    @Nullable
    private String action;

    @Nullable
    private String servletRelativeAction;

    private String method = DEFAULT_METHOD;

    @Nullable
    private String target;

    @Nullable
    private String enctype;

    @Nullable
    private String acceptCharset;

    @Nullable
    private String onsubmit;

    @Nullable
    private String onreset;

    @Nullable
    private String autocomplete;

    private String methodParam = DEFAULT_METHOD_PARAM;

    @Nullable
    private String previousNestedPath;

    public void setModelAttribute(String modelAttribute) {
        this.modelAttribute = modelAttribute;
    }

    protected String getModelAttribute() {
        return this.modelAttribute;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    @Nullable
    protected String getName() throws JspException {
        return this.name;
    }

    public void setAction(@Nullable String action) {
        this.action = (action != null ? action : "");
    }

    @Nullable
    protected String getAction() {
        return this.action;
    }

    public void setServletRelativeAction(@Nullable String servletRelativeAction) {
        this.servletRelativeAction = servletRelativeAction;
    }

    @Nullable
    protected String getServletRelativeAction() {
        return this.servletRelativeAction;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    protected String getMethod() {
        return this.method;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Nullable
    public String getTarget() {
        return this.target;
    }

    public void setEnctype(String enctype) {
        this.enctype = enctype;
    }

    @Nullable
    protected String getEnctype() {
        return this.enctype;
    }

    public void setAcceptCharset(String acceptCharset) {
        this.acceptCharset = acceptCharset;
    }

    @Nullable
    protected String getAcceptCharset() {
        return this.acceptCharset;
    }

    public void setOnsubmit(String onsubmit) {
        this.onsubmit = onsubmit;
    }

    @Nullable
    protected String getOnsubmit() {
        return this.onsubmit;
    }

    public void setOnreset(String onreset) {
        this.onreset = onreset;
    }

    @Nullable
    protected String getOnreset() {
        return this.onreset;
    }

    public void setAutocomplete(String autocomplete) {
        this.autocomplete = autocomplete;
    }

    @Nullable
    protected String getAutocomplete() {
        return this.autocomplete;
    }

    public void setMethodParam(String methodParam) {
        this.methodParam = methodParam;
    }

    protected String getMethodParam() {
        return this.methodParam;
    }

    protected boolean isMethodBrowserSupported(String method) {
        return ("get".equalsIgnoreCase(method) || "post".equalsIgnoreCase(method));
    }

    @Override
    protected int writeTagContent(TagWriter tagWriter) throws JspException {
        this.tagWriter = tagWriter;
        tagWriter.startTag(FORM_TAG);
        writeDefaultAttributes(tagWriter);
        tagWriter.writeAttribute(ACTION_ATTRIBUTE, resolveAction());
        writeOptionalAttribute(tagWriter, METHOD_ATTRIBUTE, getHttpMethod());
        writeOptionalAttribute(tagWriter, TARGET_ATTRIBUTE, getTarget());
        writeOptionalAttribute(tagWriter, ENCTYPE_ATTRIBUTE, getEnctype());
        writeOptionalAttribute(tagWriter, ACCEPT_CHARSET_ATTRIBUTE, getAcceptCharset());
        writeOptionalAttribute(tagWriter, ONSUBMIT_ATTRIBUTE, getOnsubmit());
        writeOptionalAttribute(tagWriter, ONRESET_ATTRIBUTE, getOnreset());
        writeOptionalAttribute(tagWriter, AUTOCOMPLETE_ATTRIBUTE, getAutocomplete());
        tagWriter.forceBlock();
        if (!isMethodBrowserSupported(getMethod())) {
            assertHttpMethod(getMethod());
            String inputName = getMethodParam();
            String inputType = "hidden";
            tagWriter.startTag(INPUT_TAG);
            writeOptionalAttribute(tagWriter, TYPE_ATTRIBUTE, inputType);
            writeOptionalAttribute(tagWriter, NAME_ATTRIBUTE, inputName);
            writeOptionalAttribute(tagWriter, VALUE_ATTRIBUTE, processFieldValue(inputName, getMethod(), inputType));
            tagWriter.endTag();
        }
        // Expose the form object name for nested tags...
        String modelAttribute = resolveModelAttribute();
        this.pageContext.setAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, modelAttribute, PageContext.REQUEST_SCOPE);
        // Save previous nestedPath value, build and expose current nestedPath value.
        // Use request scope to expose nestedPath to included pages too.
        this.previousNestedPath = (String) this.pageContext.getAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, modelAttribute + PropertyAccessor.NESTED_PROPERTY_SEPARATOR, PageContext.REQUEST_SCOPE);
        return EVAL_BODY_INCLUDE;
    }

    private String getHttpMethod() {
        return (isMethodBrowserSupported(getMethod()) ? getMethod() : DEFAULT_METHOD);
    }

    private void assertHttpMethod(String method) {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            if (httpMethod.name().equalsIgnoreCase(method)) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid HTTP method: " + method);
    }

    @Override
    protected String autogenerateId() throws JspException {
        return resolveModelAttribute();
    }

    protected String resolveModelAttribute() throws JspException {
        Object resolvedModelAttribute = evaluate(MODEL_ATTRIBUTE, getModelAttribute());
        if (resolvedModelAttribute == null) {
            throw new IllegalArgumentException(MODEL_ATTRIBUTE + " must not be null");
        }
        return (String) resolvedModelAttribute;
    }

    protected String resolveAction() throws JspException {
        String action = getAction();
        String servletRelativeAction = getServletRelativeAction();
        if (StringUtils.hasText(action)) {
            action = getDisplayString(evaluate(ACTION_ATTRIBUTE, action));
            return processAction(action);
        } else if (StringUtils.hasText(servletRelativeAction)) {
            String pathToServlet = getRequestContext().getPathToServlet();
            if (servletRelativeAction.startsWith("/") && !servletRelativeAction.startsWith(getRequestContext().getContextPath())) {
                servletRelativeAction = pathToServlet + servletRelativeAction;
            }
            servletRelativeAction = getDisplayString(evaluate(ACTION_ATTRIBUTE, servletRelativeAction));
            return processAction(servletRelativeAction);
        } else {
            String requestUri = getRequestContext().getRequestUri();
            String encoding = this.pageContext.getResponse().getCharacterEncoding();
            try {
                requestUri = UriUtils.encodePath(requestUri, encoding);
            } catch (UnsupportedCharsetException ex) {
                // shouldn't happen - if it does, proceed with requestUri as-is
            }
            ServletResponse response = this.pageContext.getResponse();
            if (response instanceof HttpServletResponse) {
                requestUri = ((HttpServletResponse) response).encodeURL(requestUri);
                String queryString = getRequestContext().getQueryString();
                if (StringUtils.hasText(queryString)) {
                    requestUri += "?" + HtmlUtils.htmlEscape(queryString);
                }
            }
            if (StringUtils.hasText(requestUri)) {
                return processAction(requestUri);
            } else {
                throw new IllegalArgumentException("Attribute 'action' is required. " + "Attempted to resolve against current request URI but request URI was null.");
            }
        }
    }

    private String processAction(String action) {
        RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
        ServletRequest request = this.pageContext.getRequest();
        if (processor != null && request instanceof HttpServletRequest) {
            action = processor.processAction((HttpServletRequest) request, action, getHttpMethod());
        }
        return action;
    }

    @Override
    public int doEndTag() throws JspException {
        RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
        ServletRequest request = this.pageContext.getRequest();
        if (processor != null && request instanceof HttpServletRequest) {
            writeHiddenFields(processor.getExtraHiddenFields((HttpServletRequest) request));
        }
        Assert.state(this.tagWriter != null, "No TagWriter set");
        this.tagWriter.endTag();
        return EVAL_PAGE;
    }

    private void writeHiddenFields(@Nullable Map<String, String> hiddenFields) throws JspException {
        if (!CollectionUtils.isEmpty(hiddenFields)) {
            Assert.state(this.tagWriter != null, "No TagWriter set");
            this.tagWriter.appendValue("<div>\n");
            for (Map.Entry<String, String> entry : hiddenFields.entrySet()) {
                this.tagWriter.appendValue("<input type=\"hidden\" ");
                this.tagWriter.appendValue("name=\"" + entry.getKey() + "\" value=\"" + entry.getValue() + "\" ");
                this.tagWriter.appendValue("/>\n");
            }
            this.tagWriter.appendValue("</div>");
        }
    }

    @Override
    public void doFinally() {
        super.doFinally();
        this.pageContext.removeAttribute(MODEL_ATTRIBUTE_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        if (this.previousNestedPath != null) {
            // Expose previous nestedPath value.
            this.pageContext.setAttribute(NESTED_PATH_VARIABLE_NAME, this.previousNestedPath, PageContext.REQUEST_SCOPE);
        } else {
            // Remove exposed nestedPath value.
            this.pageContext.removeAttribute(NESTED_PATH_VARIABLE_NAME, PageContext.REQUEST_SCOPE);
        }
        this.tagWriter = null;
        this.previousNestedPath = null;
    }

    @Override
    protected String resolveCssClass() throws JspException {
        return ObjectUtils.getDisplayString(evaluate("cssClass", getCssClass()));
    }

    @Override
    public void setPath(String path) {
        throw new UnsupportedOperationException("The 'path' attribute is not supported for forms");
    }

    @Override
    public void setCssErrorClass(String cssErrorClass) {
        throw new UnsupportedOperationException("The 'cssErrorClass' attribute is not supported for forms");
    }

}
