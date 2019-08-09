package org.springframework.cache;

import org.springframework.lang.Nullable;

import java.util.concurrent.Callable;

public interface Cache {

    String getName();

    Object getNativeCache();

    @Nullable
    ValueWrapper get(Object key);

    @Nullable
    <T> T get(Object key, @Nullable Class<T> type);

    @Nullable
    <T> T get(Object key, Callable<T> valueLoader);

    void put(Object key, @Nullable Object value);

    @Nullable
    ValueWrapper putIfAbsent(Object key, @Nullable Object value);

    void evict(Object key);

    void clear();

    @FunctionalInterface
    interface ValueWrapper {

        @Nullable
        Object get();

    }

    @SuppressWarnings("serial")
    class ValueRetrievalException extends RuntimeException {

        @Nullable
        private final Object key;

        public ValueRetrievalException(@Nullable Object key, Callable<?> loader, Throwable ex) {
            super(String.format("Value for key '%s' could not be loaded using '%s'", key, loader), ex);
            this.key = key;
        }

        @Nullable
        public Object getKey() {
            return this.key;
        }

    }

}
