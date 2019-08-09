package org.springframework.context;

import org.springframework.lang.Nullable;

@FunctionalInterface
public interface MessageSourceResolvable {

    @Nullable
    String[] getCodes();

    @Nullable
    default Object[] getArguments() {
        return null;
    }

    @Nullable
    default String getDefaultMessage() {
        return null;
    }

}
