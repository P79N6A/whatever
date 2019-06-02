package mmp.nio.channels.spi;

import mmp.nio.channels.SelectionKey;
import mmp.nio.channels.Selector;
import sun.nio.ch.Interruptible;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractSelector extends Selector {

    private AtomicBoolean selectorOpen = new AtomicBoolean(true);

    private final SelectorProvider provider;

    protected AbstractSelector(SelectorProvider provider) {
        this.provider = provider;
    }

    private final Set<SelectionKey> cancelledKeys = new HashSet<SelectionKey>();

    void cancel(SelectionKey k) {
        synchronized (cancelledKeys) {
            cancelledKeys.add(k);
        }
    }

    public final void close() throws IOException {
        boolean open = selectorOpen.getAndSet(false);
        if (!open)
            return;
        implCloseSelector();
    }

    protected abstract void implCloseSelector() throws IOException;

    public final boolean isOpen() {
        return selectorOpen.get();
    }

    public final SelectorProvider provider() {
        return provider;
    }

    protected final Set<SelectionKey> cancelledKeys() {
        return cancelledKeys;
    }

    protected abstract SelectionKey register(AbstractSelectableChannel ch, int ops, Object att);

    protected final void deregister(AbstractSelectionKey key) {
        ((AbstractSelectableChannel) key.channel()).removeKey(key);
    }

    private Interruptible interruptor = null;

    protected final void begin() {
        if (interruptor == null) {
            interruptor = new Interruptible() {
                public void interrupt(Thread ignore) {
                    AbstractSelector.this.wakeup();
                }
            };
        }
        AbstractInterruptibleChannel.blockedOn(interruptor);
        Thread me = Thread.currentThread();
        if (me.isInterrupted())
            interruptor.interrupt(me);
    }

    protected final void end() {
        AbstractInterruptibleChannel.blockedOn(null);
    }

}
