package org.springframework.boot.context.properties.bind;

interface BeanPropertyBinder {

    Object bindProperty(String propertyName, Bindable<?> target);

}
