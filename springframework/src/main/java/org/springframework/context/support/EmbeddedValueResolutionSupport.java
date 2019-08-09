package org.springframework.context.support;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.lang.Nullable;
import org.springframework.util.StringValueResolver;

public class EmbeddedValueResolutionSupport implements EmbeddedValueResolverAware {

    @Nullable
    private StringValueResolver embeddedValueResolver;

    @Override
    public void setEmbeddedValueResolver(StringValueResolver resolver) {
        this.embeddedValueResolver = resolver;
    }

    @Nullable
    protected String resolveEmbeddedValue(String value) {
        return (this.embeddedValueResolver != null ? this.embeddedValueResolver.resolveStringValue(value) : value);
    }

}
