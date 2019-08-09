package org.springframework.web.servlet.tags;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.support.RequestDataValueProcessor;
import org.springframework.web.util.JavaScriptUtils;
import org.springframework.web.util.TagUtils;
import org.springframework.web.util.UriUtils;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.*;

@SuppressWarnings("serial")
public class UrlTag extends HtmlEscapingAwareTag implements ParamAware {

    private static final String URL_TEMPLATE_DELIMITER_PREFIX = "{";

    private static final String URL_TEMPLATE_DELIMITER_SUFFIX = "}";

    private static final String URL_TYPE_ABSOLUTE = "://";

    private List<Param> params = Collections.emptyList();

    private Set<String> templateParams = Collections.emptySet();

    @Nullable
    private UrlType type;

    @Nullable
    private String value;

    @Nullable
    private String context;

    @Nullable
    private String var;

    private int scope = PageContext.PAGE_SCOPE;

    private boolean javaScriptEscape = false;

    public void setValue(String value) {
        if (value.contains(URL_TYPE_ABSOLUTE)) {
            this.type = UrlType.ABSOLUTE;
            this.value = value;
        } else if (value.startsWith("/")) {
            this.type = UrlType.CONTEXT_RELATIVE;
            this.value = value;
        } else {
            this.type = UrlType.RELATIVE;
            this.value = value;
        }
    }

    public void setContext(String context) {
        if (context.startsWith("/")) {
            this.context = context;
        } else {
            this.context = "/" + context;
        }
    }

    public void setVar(String var) {
        this.var = var;
    }

    public void setScope(String scope) {
        this.scope = TagUtils.getScope(scope);
    }

    public void setJavaScriptEscape(boolean javaScriptEscape) throws JspException {
        this.javaScriptEscape = javaScriptEscape;
    }

    @Override
    public void addParam(Param param) {
        this.params.add(param);
    }

    @Override
    public int doStartTagInternal() throws JspException {
        this.params = new LinkedList<>();
        this.templateParams = new HashSet<>();
        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException {
        String url = createUrl();
        RequestDataValueProcessor processor = getRequestContext().getRequestDataValueProcessor();
        ServletRequest request = this.pageContext.getRequest();
        if ((processor != null) && (request instanceof HttpServletRequest)) {
            url = processor.processUrl((HttpServletRequest) request, url);
        }
        if (this.var == null) {
            // print the url to the writer
            try {
                this.pageContext.getOut().print(url);
            } catch (IOException ex) {
                throw new JspException(ex);
            }
        } else {
            // store the url as a variable
            this.pageContext.setAttribute(this.var, url, this.scope);
        }
        return EVAL_PAGE;
    }

    String createUrl() throws JspException {
        Assert.state(this.value != null, "No value set");
        HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();
        HttpServletResponse response = (HttpServletResponse) this.pageContext.getResponse();
        StringBuilder url = new StringBuilder();
        if (this.type == UrlType.CONTEXT_RELATIVE) {
            // add application context to url
            if (this.context == null) {
                url.append(request.getContextPath());
            } else {
                if (this.context.endsWith("/")) {
                    url.append(this.context.substring(0, this.context.length() - 1));
                } else {
                    url.append(this.context);
                }
            }
        }
        if (this.type != UrlType.RELATIVE && this.type != UrlType.ABSOLUTE && !this.value.startsWith("/")) {
            url.append("/");
        }
        url.append(replaceUriTemplateParams(this.value, this.params, this.templateParams));
        url.append(createQueryString(this.params, this.templateParams, (url.indexOf("?") == -1)));
        String urlStr = url.toString();
        if (this.type != UrlType.ABSOLUTE) {
            // Add the session identifier if needed
            // (Do not embed the session identifier in a remote link!)
            urlStr = response.encodeURL(urlStr);
        }
        // HTML and/or JavaScript escape, if demanded.
        urlStr = htmlEscape(urlStr);
        urlStr = (this.javaScriptEscape ? JavaScriptUtils.javaScriptEscape(urlStr) : urlStr);
        return urlStr;
    }

    protected String createQueryString(List<Param> params, Set<String> usedParams, boolean includeQueryStringDelimiter) throws JspException {
        String encoding = this.pageContext.getResponse().getCharacterEncoding();
        StringBuilder qs = new StringBuilder();
        for (Param param : params) {
            if (!usedParams.contains(param.getName()) && StringUtils.hasLength(param.getName())) {
                if (includeQueryStringDelimiter && qs.length() == 0) {
                    qs.append("?");
                } else {
                    qs.append("&");
                }
                try {
                    qs.append(UriUtils.encodeQueryParam(param.getName(), encoding));
                    if (param.getValue() != null) {
                        qs.append("=");
                        qs.append(UriUtils.encodeQueryParam(param.getValue(), encoding));
                    }
                } catch (UnsupportedCharsetException ex) {
                    throw new JspException(ex);
                }
            }
        }
        return qs.toString();
    }

    protected String replaceUriTemplateParams(String uri, List<Param> params, Set<String> usedParams) throws JspException {
        String encoding = this.pageContext.getResponse().getCharacterEncoding();
        for (Param param : params) {
            String template = URL_TEMPLATE_DELIMITER_PREFIX + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
            if (uri.contains(template)) {
                usedParams.add(param.getName());
                String value = param.getValue();
                try {
                    uri = StringUtils.replace(uri, template, (value != null ? UriUtils.encodePath(value, encoding) : ""));
                } catch (UnsupportedCharsetException ex) {
                    throw new JspException(ex);
                }
            } else {
                template = URL_TEMPLATE_DELIMITER_PREFIX + '/' + param.getName() + URL_TEMPLATE_DELIMITER_SUFFIX;
                if (uri.contains(template)) {
                    usedParams.add(param.getName());
                    String value = param.getValue();
                    try {
                        uri = StringUtils.replace(uri, template, (value != null ? UriUtils.encodePathSegment(value, encoding) : ""));
                    } catch (UnsupportedCharsetException ex) {
                        throw new JspException(ex);
                    }
                }
            }
        }
        return uri;
    }

    private enum UrlType {

        CONTEXT_RELATIVE, RELATIVE, ABSOLUTE
    }

}
