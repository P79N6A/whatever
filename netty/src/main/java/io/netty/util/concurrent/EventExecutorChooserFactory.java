package io.netty.util.concurrent;

import io.netty.util.internal.UnstableApi;

@UnstableApi
public interface EventExecutorChooserFactory {

    EventExecutorChooser newChooser(EventExecutor[] executors);

    @UnstableApi
    interface EventExecutorChooser {

        EventExecutor next();
    }
}
