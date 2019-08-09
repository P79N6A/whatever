package org.springframework.web.context.request;

import org.springframework.lang.Nullable;

public class RequestScope extends AbstractRequestAttributesScope {

    @Override
    protected int getScope() {
        return RequestAttributes.SCOPE_REQUEST;
    }

    @Override
    @Nullable
    public String getConversationId() {
        return null;
    }

}
