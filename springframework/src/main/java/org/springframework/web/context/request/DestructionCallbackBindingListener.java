package org.springframework.web.context.request;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.io.Serializable;

@SuppressWarnings("serial")
public class DestructionCallbackBindingListener implements HttpSessionBindingListener, Serializable {

    private final Runnable destructionCallback;

    public DestructionCallbackBindingListener(Runnable destructionCallback) {
        this.destructionCallback = destructionCallback;
    }

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        this.destructionCallback.run();
    }

}
