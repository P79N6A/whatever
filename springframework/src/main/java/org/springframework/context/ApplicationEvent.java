package org.springframework.context;

import java.util.EventObject;

public abstract class ApplicationEvent extends EventObject {

    private static final long serialVersionUID = 7099057708183571937L;

    private final long timestamp;

    public ApplicationEvent(Object source) {
        super(source);
        this.timestamp = System.currentTimeMillis();
    }

    public final long getTimestamp() {
        return this.timestamp;
    }

}
