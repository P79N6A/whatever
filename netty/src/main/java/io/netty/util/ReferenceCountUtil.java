package io.netty.util;

import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 服务端处理请求时，创建一个缓冲区ByteBuf，直到将字节数据解码为POJO对象，该缓冲区才不再有用
 * 面对并发请求时，如果能复用缓冲区而不是每次都创建，将大大提高性能：
 * 1. JAVA GC效率不高
 * 2. 缓冲区对象还需要尽可能的重用
 * Netty4引入引用计数，缓冲区的生命周期可由引用计数管理，当缓冲区不再有用时，可快速返回给对象池或者分配器用于再次分配
 * 引用计数并不专门服务于ByteBuf，用户可根据需求，在其他对象之上实现引用计数接口ReferenceCounted
 */
public final class ReferenceCountUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReferenceCountUtil.class);

    static {
        ResourceLeakDetector.addExclusions(ReferenceCountUtil.class, "touch");
    }

    private ReferenceCountUtil() {
        // 对象的初始引用计数为1，计数为0的对象不能再被使用，只能被释放
        // retain()使引用计数增加1，使用release()使引用计数减少1
        // 使用引用计数为0的对象时，将抛出异常IllegalReferenceCountException

        // 通用原则是：谁最后使用含有引用计数的对象，谁负责释放或销毁该对象

        // 通过duplicate()，slice()等等生成的派生缓冲区ByteBuf会共享原生缓冲区的内部存储区域
        // 派生缓冲区并没有自己独立的引用计数，需要共享原生缓冲区的引用计数
        // 将派生缓冲区传入下一个组件时，一定要注意先调用retain()方法

        // 由于JVM将引用计数对象当做常规对象处理，当不为0的引用计数对象变得不可达时仍然会被GC，造成内存泄露
        // Netty提供了相应的检测机制并定义了四个检测级别
        // DISABLED 完全关闭内存泄露检测，不建议
        // SIMPLE 以1%的抽样率检测是否泄露，默认级别
        // ADVANCED 抽样率同SIMPLE，但显示详细的泄露报告
        // PARANOID 抽样率为100%，显示报告信息同ADVANCED
    }

    @SuppressWarnings("unchecked")
    public static <T> T retain(T msg) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).retain();
        }
        return msg;
    }

    @SuppressWarnings("unchecked")
    public static <T> T retain(T msg, int increment) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).retain(increment);
        }
        return msg;
    }

    @SuppressWarnings("unchecked")
    public static <T> T touch(T msg) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).touch();
        }
        return msg;
    }

    @SuppressWarnings("unchecked")
    public static <T> T touch(T msg, Object hint) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).touch(hint);
        }
        return msg;
    }

    public static boolean release(Object msg) {
        if (msg instanceof ReferenceCounted) {
            return ((ReferenceCounted) msg).release();
        }
        return false;
    }

    public static boolean release(Object msg, int decrement) {
        if (msg instanceof ReferenceCounted) {
            return ((ReferenceCounted) msg).release(decrement);
        }
        return false;
    }

    public static void safeRelease(Object msg) {
        try {
            release(msg);
        } catch (Throwable t) {
            logger.warn("Failed to release a message: {}", msg, t);
        }
    }

    public static void safeRelease(Object msg, int decrement) {
        try {
            release(msg, decrement);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to release a message: {} (decrement: {})", msg, decrement, t);
            }
        }
    }

    /**
     * 通用方法，当线程结束时，将会自动释放缓冲区
     * ByteBuf buf = ReferenceCountUtil.releaseLater(Unpooled.directBuffer(512));
     */
    @Deprecated
    public static <T> T releaseLater(T msg) {
        return releaseLater(msg, 1);
    }

    @Deprecated
    public static <T> T releaseLater(T msg, int decrement) {
        if (msg instanceof ReferenceCounted) {
            ThreadDeathWatcher.watch(Thread.currentThread(), new ReleasingTask((ReferenceCounted) msg, decrement));
        }
        return msg;
    }

    public static int refCnt(Object msg) {
        return msg instanceof ReferenceCounted ? ((ReferenceCounted) msg).refCnt() : -1;
    }

    private static final class ReleasingTask implements Runnable {

        private final ReferenceCounted obj;
        private final int decrement;

        ReleasingTask(ReferenceCounted obj, int decrement) {
            this.obj = obj;
            this.decrement = decrement;
        }

        @Override
        public void run() {
            try {
                if (!obj.release(decrement)) {
                    logger.warn("Non-zero refCnt: {}", this);
                } else {
                    logger.debug("Released: {}", this);
                }
            } catch (Exception ex) {
                logger.warn("Failed to release an object: {}", obj, ex);
            }
        }

        @Override
        public String toString() {
            return StringUtil.simpleClassName(obj) + ".release(" + decrement + ") refCnt: " + obj.refCnt();
        }
    }
}
