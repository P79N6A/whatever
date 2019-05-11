package mmp.nio.channels;

import mmp.nio.channels.spi.SelectorProvider;

import java.io.Closeable;
import java.io.IOException;
import java.util.Set;


public abstract class Selector implements Closeable {

    protected Selector() {
    }

    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    public abstract boolean isOpen();

    public abstract SelectorProvider provider();

    public abstract Set<SelectionKey> keys();

    public abstract Set<SelectionKey> selectedKeys();

    // 立即返回不阻塞
    public abstract int selectNow() throws IOException;

    // 直到指定时间或者就绪
    // 返回的int值表示有多少通道已经就绪
    // 即，自上次调用select()方法后有多少通道变成就绪状态
    public abstract int select(long timeout) throws IOException;

    // 阻塞到至少有一个通道在注册的事件上就绪了
    public abstract int select() throws IOException;


    // 某个线程调用select()方法后阻塞了，其它线程调用wakeup()使阻塞在select()方法上的线程马上返回
    // 如果有其它线程调用了wakeup()方法，但当前没有线程阻塞在select()方法上，下个调用select()方法的线程会立即wakeup
    public abstract Selector wakeup();

    // 关闭该Selector，且使注册到该Selector上的所有SelectionKey实例无效
    // 通道本身并不会关闭
    public abstract void close() throws IOException;

}
