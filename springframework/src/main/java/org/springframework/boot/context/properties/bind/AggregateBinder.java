package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;

import java.util.function.Supplier;

abstract class AggregateBinder<T> {

    private final Context context;

    AggregateBinder(Context context) {
        this.context = context;
    }

    protected abstract boolean isAllowRecursiveBinding(ConfigurationPropertySource source);

    @SuppressWarnings("unchecked")
    public final Object bind(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder) {
        Object result = bindAggregate(name, target, elementBinder);
        Supplier<?> value = target.getValue();
        if (result == null || value == null) {
            return result;
        }
        return merge((Supplier<T>) value, (T) result);
    }

    protected abstract Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder);

    protected abstract T merge(Supplier<T> existing, T additional);

    protected final Context getContext() {
        return this.context;
    }

    protected static class AggregateSupplier<T> {

        private final Supplier<T> supplier;

        private T supplied;

        public AggregateSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public T get() {
            if (this.supplied == null) {
                this.supplied = this.supplier.get();
            }
            return this.supplied;
        }

        public boolean wasSupplied() {
            return this.supplied != null;
        }

    }

}
