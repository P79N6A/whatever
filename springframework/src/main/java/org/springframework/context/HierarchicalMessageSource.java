package org.springframework.context;

import org.springframework.lang.Nullable;

public interface HierarchicalMessageSource extends MessageSource {

    void setParentMessageSource(@Nullable MessageSource parent);

    @Nullable
    MessageSource getParentMessageSource();

}
