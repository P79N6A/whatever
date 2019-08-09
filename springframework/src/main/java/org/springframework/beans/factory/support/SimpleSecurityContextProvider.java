package org.springframework.beans.factory.support;

import org.springframework.lang.Nullable;

import java.security.AccessControlContext;
import java.security.AccessController;

public class SimpleSecurityContextProvider implements SecurityContextProvider {

    @Nullable
    private final AccessControlContext acc;

    public SimpleSecurityContextProvider() {
        this(null);
    }

    public SimpleSecurityContextProvider(@Nullable AccessControlContext acc) {
        this.acc = acc;
    }

    @Override
    public AccessControlContext getAccessControlContext() {
        return (this.acc != null ? this.acc : AccessController.getContext());
    }

}
