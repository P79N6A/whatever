package org.springframework.web.bind.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.WebRequest;

public class DefaultSessionAttributeStore implements SessionAttributeStore {

    private String attributeNamePrefix = "";

    public void setAttributeNamePrefix(@Nullable String attributeNamePrefix) {
        this.attributeNamePrefix = (attributeNamePrefix != null ? attributeNamePrefix : "");
    }

    @Override
    public void storeAttribute(WebRequest request, String attributeName, Object attributeValue) {
        Assert.notNull(request, "WebRequest must not be null");
        Assert.notNull(attributeName, "Attribute name must not be null");
        Assert.notNull(attributeValue, "Attribute value must not be null");
        String storeAttributeName = getAttributeNameInSession(request, attributeName);
        request.setAttribute(storeAttributeName, attributeValue, WebRequest.SCOPE_SESSION);
    }

    @Override
    @Nullable
    public Object retrieveAttribute(WebRequest request, String attributeName) {
        Assert.notNull(request, "WebRequest must not be null");
        Assert.notNull(attributeName, "Attribute name must not be null");
        String storeAttributeName = getAttributeNameInSession(request, attributeName);
        return request.getAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
    }

    @Override
    public void cleanupAttribute(WebRequest request, String attributeName) {
        Assert.notNull(request, "WebRequest must not be null");
        Assert.notNull(attributeName, "Attribute name must not be null");
        String storeAttributeName = getAttributeNameInSession(request, attributeName);
        request.removeAttribute(storeAttributeName, WebRequest.SCOPE_SESSION);
    }

    protected String getAttributeNameInSession(WebRequest request, String attributeName) {
        return this.attributeNamePrefix + attributeName;
    }

}
