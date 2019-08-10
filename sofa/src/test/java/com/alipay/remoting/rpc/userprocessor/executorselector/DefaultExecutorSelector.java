package com.alipay.remoting.rpc.userprocessor.executorselector;

import com.alipay.remoting.NamedThreadFactory;
import com.alipay.remoting.rpc.protocol.UserProcessor;
import com.alipay.remoting.util.StringUtils;
import org.junit.Assert;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultExecutorSelector implements UserProcessor.ExecutorSelector {
    public static final String EXECUTOR0 = "executor0";

    public static final String EXECUTOR1 = "executor1";

    private String chooseExecutorStr;

    private ThreadPoolExecutor executor0;

    private ThreadPoolExecutor executor1;

    public DefaultExecutorSelector(String chooseExecutorStr) {
        this.chooseExecutorStr = chooseExecutorStr;
        this.executor0 = new ThreadPoolExecutor(1, 3, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(4), new NamedThreadFactory("Rpc-specific0-executor"));
        this.executor1 = new ThreadPoolExecutor(1, 3, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(4), new NamedThreadFactory("Rpc-specific1-executor"));
    }

    @Override
    public Executor select(String requestClass, Object requestHeader) {
        Assert.assertNotNull(requestClass);
        Assert.assertNotNull(requestHeader);
        if (StringUtils.equals(chooseExecutorStr, (String) requestHeader)) {
            return executor1;
        } else {
            return executor0;
        }
    }

}