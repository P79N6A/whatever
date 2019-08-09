package org.springframework.web.servlet.support;

import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class JspAwareRequestContext extends RequestContext {

    private PageContext pageContext;

    public JspAwareRequestContext(PageContext pageContext) {
        this(pageContext, null);
    }

    public JspAwareRequestContext(PageContext pageContext, @Nullable Map<String, Object> model) {
        super((HttpServletRequest) pageContext.getRequest(), (HttpServletResponse) pageContext.getResponse(), pageContext.getServletContext(), model);
        this.pageContext = pageContext;
    }

    protected final PageContext getPageContext() {
        return this.pageContext;
    }

    @Override
    protected Locale getFallbackLocale() {
        if (jstlPresent) {
            Locale locale = JstlPageLocaleResolver.getJstlLocale(getPageContext());
            if (locale != null) {
                return locale;
            }
        }
        return getRequest().getLocale();
    }

    @Override
    protected TimeZone getFallbackTimeZone() {
        if (jstlPresent) {
            TimeZone timeZone = JstlPageLocaleResolver.getJstlTimeZone(getPageContext());
            if (timeZone != null) {
                return timeZone;
            }
        }
        return null;
    }

    private static class JstlPageLocaleResolver {

        @Nullable
        public static Locale getJstlLocale(PageContext pageContext) {
            Object localeObject = Config.find(pageContext, Config.FMT_LOCALE);
            return (localeObject instanceof Locale ? (Locale) localeObject : null);
        }

        @Nullable
        public static TimeZone getJstlTimeZone(PageContext pageContext) {
            Object timeZoneObject = Config.find(pageContext, Config.FMT_TIME_ZONE);
            return (timeZoneObject instanceof TimeZone ? (TimeZone) timeZoneObject : null);
        }

    }

}
