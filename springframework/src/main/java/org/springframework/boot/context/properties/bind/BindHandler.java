package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

public interface BindHandler {

    BindHandler DEFAULT = new BindHandler() {

    };

    default <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        return target;
    }

    default Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        return result;
    }

    default Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error) throws Exception {
        throw error;
    }

    default void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
    }

}
