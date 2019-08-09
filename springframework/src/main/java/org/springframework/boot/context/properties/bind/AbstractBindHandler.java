package org.springframework.boot.context.properties.bind;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.util.Assert;

public abstract class AbstractBindHandler implements BindHandler {

    private final BindHandler parent;

    public AbstractBindHandler() {
        this(BindHandler.DEFAULT);
    }

    public AbstractBindHandler(BindHandler parent) {
        Assert.notNull(parent, "Parent must not be null");
        this.parent = parent;
    }

    @Override
    public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        return this.parent.onStart(name, target, context);
    }

    @Override
    public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        return this.parent.onSuccess(name, target, context, result);
    }

    @Override
    public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error) throws Exception {
        return this.parent.onFailure(name, target, context, error);
    }

    @Override
    public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
        this.parent.onFinish(name, target, context, result);
    }

}
