package mmp.nio.channels.spi;

import mmp.nio.channels.AsynchronousChannelGroup;
import mmp.nio.channels.AsynchronousServerSocketChannel;
import mmp.nio.channels.AsynchronousSocketChannel;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

public abstract class AsynchronousChannelProvider {
    private static Void checkPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("asynchronousChannelProvider"));
        return null;
    }

    private AsynchronousChannelProvider(Void ignore) {
    }

    protected AsynchronousChannelProvider() {
        this(checkPermission());
    }

    private static class ProviderHolder {
        static final AsynchronousChannelProvider provider = load();

        private static AsynchronousChannelProvider load() {
            return null;
        }

    }

    public static AsynchronousChannelProvider provider() {
        return ProviderHolder.provider;
    }

    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads, ThreadFactory threadFactory) throws IOException;

    public abstract AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor, int initialSize) throws IOException;

    public abstract AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(AsynchronousChannelGroup group) throws IOException;

    public abstract AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group) throws IOException;
}
