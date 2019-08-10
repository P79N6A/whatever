package com.alipay.remoting.rpc.protocol;

import com.alipay.remoting.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;

public class UserProcessorRegisterHelper {

    public static void registerUserProcessor(UserProcessor<?> processor, ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        if (null == processor) {
            throw new RuntimeException("User processor should not be null!");
        }
        if (processor instanceof MultiInterestUserProcessor) {
            registerUserProcessor((MultiInterestUserProcessor) processor, userProcessors);
        } else {
            if (StringUtils.isBlank(processor.interest())) {
                throw new RuntimeException("Processor interest should not be blank!");
            }
            UserProcessor<?> preProcessor = userProcessors.putIfAbsent(processor.interest(), processor);
            if (preProcessor != null) {
                String errMsg = "Processor with interest key [" + processor.interest() + "] has already been registered to rpc server, can not register again!";
                throw new RuntimeException(errMsg);
            }
        }

    }

    private static void registerUserProcessor(MultiInterestUserProcessor<?> processor, ConcurrentHashMap<String, UserProcessor<?>> userProcessors) {
        if (null == processor.multiInterest() || processor.multiInterest().isEmpty()) {
            throw new RuntimeException("Processor interest should not be blank!");
        }
        for (String interest : processor.multiInterest()) {
            UserProcessor<?> preProcessor = userProcessors.putIfAbsent(interest, processor);
            if (preProcessor != null) {
                String errMsg = "Processor with interest key [" + interest + "] has already been registered to rpc server, can not register again!";
                throw new RuntimeException(errMsg);
            }
        }

    }

}
