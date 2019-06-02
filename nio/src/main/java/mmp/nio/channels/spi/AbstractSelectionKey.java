package mmp.nio.channels.spi;

import mmp.nio.channels.SelectionKey;

public abstract class AbstractSelectionKey extends SelectionKey {

    protected AbstractSelectionKey() {
    }

    private volatile boolean valid = true;

    public final boolean isValid() {
        return valid;
    }

    void invalidate() {
        valid = false;
    }

    public final void cancel() {

        synchronized (this) {
            if (valid) {
                valid = false;
                ((AbstractSelector) selector()).cancel(this);
            }
        }
    }
}
