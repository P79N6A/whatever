package org.springframework.boot.autoconfigure.template;

import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.AbstractTemplateViewResolver;

public abstract class AbstractTemplateViewResolverProperties extends AbstractViewResolverProperties {

    private String prefix;

    private String suffix;

    private String requestContextAttribute;

    private boolean exposeRequestAttributes = false;

    private boolean exposeSessionAttributes = false;

    private boolean allowRequestOverride = false;

    private boolean exposeSpringMacroHelpers = true;

    private boolean allowSessionOverride = false;

    protected AbstractTemplateViewResolverProperties(String defaultPrefix, String defaultSuffix) {
        this.prefix = defaultPrefix;
        this.suffix = defaultSuffix;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getRequestContextAttribute() {
        return this.requestContextAttribute;
    }

    public void setRequestContextAttribute(String requestContextAttribute) {
        this.requestContextAttribute = requestContextAttribute;
    }

    public boolean isExposeRequestAttributes() {
        return this.exposeRequestAttributes;
    }

    public void setExposeRequestAttributes(boolean exposeRequestAttributes) {
        this.exposeRequestAttributes = exposeRequestAttributes;
    }

    public boolean isExposeSessionAttributes() {
        return this.exposeSessionAttributes;
    }

    public void setExposeSessionAttributes(boolean exposeSessionAttributes) {
        this.exposeSessionAttributes = exposeSessionAttributes;
    }

    public boolean isAllowRequestOverride() {
        return this.allowRequestOverride;
    }

    public void setAllowRequestOverride(boolean allowRequestOverride) {
        this.allowRequestOverride = allowRequestOverride;
    }

    public boolean isAllowSessionOverride() {
        return this.allowSessionOverride;
    }

    public void setAllowSessionOverride(boolean allowSessionOverride) {
        this.allowSessionOverride = allowSessionOverride;
    }

    public boolean isExposeSpringMacroHelpers() {
        return this.exposeSpringMacroHelpers;
    }

    public void setExposeSpringMacroHelpers(boolean exposeSpringMacroHelpers) {
        this.exposeSpringMacroHelpers = exposeSpringMacroHelpers;
    }

    public void applyToMvcViewResolver(Object viewResolver) {
        Assert.isInstanceOf(AbstractTemplateViewResolver.class, viewResolver, "ViewResolver is not an instance of AbstractTemplateViewResolver :" + viewResolver);
        AbstractTemplateViewResolver resolver = (AbstractTemplateViewResolver) viewResolver;
        resolver.setPrefix(getPrefix());
        resolver.setSuffix(getSuffix());
        resolver.setCache(isCache());
        if (getContentType() != null) {
            resolver.setContentType(getContentType().toString());
        }
        resolver.setViewNames(getViewNames());
        resolver.setExposeRequestAttributes(isExposeRequestAttributes());
        resolver.setAllowRequestOverride(isAllowRequestOverride());
        resolver.setAllowSessionOverride(isAllowSessionOverride());
        resolver.setExposeSessionAttributes(isExposeSessionAttributes());
        resolver.setExposeSpringMacroHelpers(isExposeSpringMacroHelpers());
        resolver.setRequestContextAttribute(getRequestContextAttribute());
        // The resolver usually acts as a fallback resolver (e.g. like a
        // InternalResourceViewResolver) so it needs to have low precedence
        resolver.setOrder(Ordered.LOWEST_PRECEDENCE - 5);
    }

}
