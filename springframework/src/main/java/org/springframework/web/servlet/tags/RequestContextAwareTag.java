package org.springframework.web.servlet.tags;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.servlet.support.JspAwareRequestContext;
import org.springframework.web.servlet.support.RequestContext;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.TagSupport;
import javax.servlet.jsp.tagext.TryCatchFinally;

@SuppressWarnings("serial")
public abstract class RequestContextAwareTag extends TagSupport implements TryCatchFinally {

    public static final String REQUEST_CONTEXT_PAGE_ATTRIBUTE = "org.springframework.web.servlet.tags.REQUEST_CONTEXT";

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private RequestContext requestContext;

    @Override
    public final int doStartTag() throws JspException {
        try {
            this.requestContext = (RequestContext) this.pageContext.getAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE);
            if (this.requestContext == null) {
                this.requestContext = new JspAwareRequestContext(this.pageContext);
                this.pageContext.setAttribute(REQUEST_CONTEXT_PAGE_ATTRIBUTE, this.requestContext);
            }
            return doStartTagInternal();
        } catch (JspException | RuntimeException ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw new JspTagException(ex.getMessage());
        }
    }

    protected final RequestContext getRequestContext() {
        Assert.state(this.requestContext != null, "No current RequestContext");
        return this.requestContext;
    }

    protected abstract int doStartTagInternal() throws Exception;

    @Override
    public void doCatch(Throwable throwable) throws Throwable {
        throw throwable;
    }

    @Override
    public void doFinally() {
        this.requestContext = null;
    }

}
