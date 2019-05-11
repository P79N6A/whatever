package mmp.nio.channels.spi;

import mmp.nio.channels.DatagramChannel;
import mmp.nio.channels.Pipe;
import mmp.nio.channels.ServerSocketChannel;
import mmp.nio.channels.SocketChannel;

import java.io.IOException;
import java.net.ProtocolFamily;


public abstract class SelectorProvider {

    private static final Object lock = new Object();
    private static SelectorProvider provider = null;


    protected SelectorProvider() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
            sm.checkPermission(new RuntimePermission("selectorProvider"));
    }


    public static SelectorProvider provider() {
        synchronized (lock) {
            if (provider != null)
                return provider;
            return null;
        }
    }


    public abstract DatagramChannel openDatagramChannel() throws IOException;


    public abstract DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException;


    public abstract Pipe openPipe() throws IOException;


    public abstract AbstractSelector openSelector() throws IOException;


    public abstract ServerSocketChannel openServerSocketChannel() throws IOException;


    public abstract SocketChannel openSocketChannel() throws IOException;


}
