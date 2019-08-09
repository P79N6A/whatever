package org.springframework.scheduling.concurrent;

import org.springframework.util.CustomizableThreadCreator;

import java.util.concurrent.ThreadFactory;

@SuppressWarnings("serial")
public class CustomizableThreadFactory extends CustomizableThreadCreator implements ThreadFactory {

    public CustomizableThreadFactory() {
        super();
    }

    public CustomizableThreadFactory(String threadNamePrefix) {
        super(threadNamePrefix);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return createThread(runnable);
    }

}
