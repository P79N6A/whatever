package org.springframework.scheduling.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class ExecutorBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected String getBeanClassName(Element element) {
        return "org.springframework.scheduling.config.TaskExecutorFactoryBean";
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        String keepAliveSeconds = element.getAttribute("keep-alive");
        if (StringUtils.hasText(keepAliveSeconds)) {
            builder.addPropertyValue("keepAliveSeconds", keepAliveSeconds);
        }
        String queueCapacity = element.getAttribute("queue-capacity");
        if (StringUtils.hasText(queueCapacity)) {
            builder.addPropertyValue("queueCapacity", queueCapacity);
        }
        configureRejectionPolicy(element, builder);
        String poolSize = element.getAttribute("pool-size");
        if (StringUtils.hasText(poolSize)) {
            builder.addPropertyValue("poolSize", poolSize);
        }
    }

    private void configureRejectionPolicy(Element element, BeanDefinitionBuilder builder) {
        String rejectionPolicy = element.getAttribute("rejection-policy");
        if (!StringUtils.hasText(rejectionPolicy)) {
            return;
        }
        String prefix = "java.util.concurrent.ThreadPoolExecutor.";
        String policyClassName;
        if (rejectionPolicy.equals("ABORT")) {
            policyClassName = prefix + "AbortPolicy";
        } else if (rejectionPolicy.equals("CALLER_RUNS")) {
            policyClassName = prefix + "CallerRunsPolicy";
        } else if (rejectionPolicy.equals("DISCARD")) {
            policyClassName = prefix + "DiscardPolicy";
        } else if (rejectionPolicy.equals("DISCARD_OLDEST")) {
            policyClassName = prefix + "DiscardOldestPolicy";
        } else {
            policyClassName = rejectionPolicy;
        }
        builder.addPropertyValue("rejectedExecutionHandler", new RootBeanDefinition(policyClassName));
    }

}
