package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.bind.Binder.Context;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

interface BeanBinder {

    <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Context context, BeanPropertyBinder propertyBinder);

}
