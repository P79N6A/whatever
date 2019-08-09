package org.springframework.util.function;

import org.springframework.lang.Nullable;

import java.util.function.Supplier;

public abstract class SupplierUtils {

    @Nullable
    public static <T> T resolve(@Nullable Supplier<T> supplier) {
        return (supplier != null ? supplier.get() : null);
    }

}
