package com.alipay.remoting.rpc.userprocessor;

import com.alipay.remoting.BizContext;
import com.alipay.remoting.rpc.common.SimpleServerUserProcessor;
import com.alipay.remoting.rpc.protocol.*;
import com.alipay.remoting.rpc.userprocessor.multiinterestprocessor.SimpleServerMultiInterestUserProcessor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserProcessorRegisterHelperTest {

    ConcurrentHashMap<String, UserProcessor<?>> userProcessors;

    @Before
    public void init() {
        userProcessors = new ConcurrentHashMap<String, UserProcessor<?>>();
    }

    @Test
    public void testRegisterUserProcessor() {
        UserProcessor userProcessor = new SimpleServerUserProcessor();
        UserProcessorRegisterHelper.registerUserProcessor(userProcessor, userProcessors);
        Assert.assertEquals(1, userProcessors.size());
    }

    @Test
    public void testRegisterMultiInterestUserProcessor() {
        UserProcessor multiInterestUserProcessor = new SimpleServerMultiInterestUserProcessor();
        UserProcessorRegisterHelper.registerUserProcessor(multiInterestUserProcessor, userProcessors);
        Assert.assertEquals(((SimpleServerMultiInterestUserProcessor) multiInterestUserProcessor).multiInterest().size(), userProcessors.size());
    }

    @Test
    public void testInterestNullException() {
        UserProcessor userProcessor = new SyncUserProcessor() {
            @Override
            public Object handleRequest(BizContext bizCtx, Object request) throws Exception {
                return request;
            }

            @Override
            public String interest() {
                return null;
            }
        };
        try {
            UserProcessorRegisterHelper.registerUserProcessor(userProcessor, userProcessors);
        } catch (RuntimeException e) {
        }
        Assert.assertEquals(0, userProcessors.size());
    }

    @Test
    public void testInterestEmptyException() {
        MultiInterestUserProcessor userProcessor = new SyncMutiInterestUserProcessor() {
            @Override
            public Object handleRequest(BizContext bizCtx, Object request) throws Exception {
                return request;
            }

            @Override
            public List<String> multiInterest() {
                return new ArrayList<String>();
            }

        };
        try {
            UserProcessorRegisterHelper.registerUserProcessor(userProcessor, userProcessors);
        } catch (RuntimeException e) {
        }
        Assert.assertEquals(0, userProcessors.size());
    }

    @Test
    public void testInterestRepeatException() {
        UserProcessor userProcessor = new SimpleServerUserProcessor();
        UserProcessor repeatedUserProcessor = new SimpleServerUserProcessor();
        try {
            UserProcessorRegisterHelper.registerUserProcessor(userProcessor, userProcessors);
            UserProcessorRegisterHelper.registerUserProcessor(repeatedUserProcessor, userProcessors);
        } catch (RuntimeException e) {
        }
        Assert.assertEquals(1, userProcessors.size());
    }

}
