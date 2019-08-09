package org.springframework.beans;

import org.springframework.lang.Nullable;

public interface Mergeable {

    boolean isMergeEnabled();

    Object merge(@Nullable Object parent);

}
