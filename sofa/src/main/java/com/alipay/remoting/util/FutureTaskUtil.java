package com.alipay.remoting.util;

import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;

public class FutureTaskUtil {

    public static <T> T getFutureTaskResult(RunStateRecordedFutureTask<T> task, Logger logger) {
        T t = null;
        if (null != task) {
            try {
                t = task.getAfterRun();
            } catch (InterruptedException e) {
                logger.error("Future task interrupted!", e);
            } catch (ExecutionException e) {
                logger.error("Future task execute failed!", e);
            } catch (FutureTaskNotRunYetException e) {
                logger.error("Future task has not run yet!", e);
            } catch (FutureTaskNotCompleted e) {
                logger.error("Future task has not completed!", e);
            }
        }
        return t;
    }

    public static void launderThrowable(Throwable t) {
        if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new IllegalStateException("Not unchecked!", t);
        }
    }

}