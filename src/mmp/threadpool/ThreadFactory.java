package mmp.threadpool;

import java.lang.Thread;

public interface ThreadFactory {

    Thread newThread(Runnable r);
}
