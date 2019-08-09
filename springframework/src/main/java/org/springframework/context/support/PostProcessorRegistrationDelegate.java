package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

import java.util.*;

final class PostProcessorRegistrationDelegate {

    private PostProcessorRegistrationDelegate() {
    }

    /**
     * 执行BeanFactoryPostProcessor
     */
    public static void invokeBeanFactoryPostProcessors(ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {
        // 存放已处理的Bean
        Set<String> processedBeans = new HashSet<>();
        // 如果BeanFactory实现了BeanDefinitionRegistry
        if (beanFactory instanceof BeanDefinitionRegistry) {
            // BeanFactory转型
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
            List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
            // 遍历BeanFactoryPostProcessor列表
            for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
                // 如果是BeanDefinitionRegistryPostProcessor
                if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                    // 转型
                    BeanDefinitionRegistryPostProcessor registryProcessor = (BeanDefinitionRegistryPostProcessor) postProcessor;
                    // 注解方式，这里会处理@ComponentScan注解并载入BeanDefinition
                    // BeanDefinitionRegistryPostProcessor#postProcessBeanDefinitionRegistry
                    registryProcessor.postProcessBeanDefinitionRegistry(registry);
                    // 实现BeanDefinitionRegistryPostProcessor接口的添加到registryPostProcessors列表
                    registryProcessors.add(registryProcessor);
                } else {
                    // 只实现BeanFactoryPostProcessor接口的添加到regularPostProcessors列表
                    regularPostProcessors.add(postProcessor);
                }
            }

            /*
             * 这里不初始化FactoryBeans，因为要让BeanFactoryPostProcessor处理普通Bean
             */
            // 按PriorityOrdered，Ordered，其他的顺序分别初始化
            List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

            // 从beanDefinitionNames和manualSingletonNames获取已注册的BeanDefinitionRegistryPostProcessor的beanName
            String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                    // 实现了PriorityOrdered接口的先实例化，并添加到currentRegistryProcessors列表
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    // 名字加到processedBeans列表
                    processedBeans.add(ppName);
                }
            }
            // 排序
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            // 添加currentRegistryProcessors到registryProcessors
            registryProcessors.addAll(currentRegistryProcessors);
            // 遍历currentRegistryProcessors列表，执行postProcessBeanDefinitionRegistry
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            // 清空currentRegistryProcessors
            currentRegistryProcessors.clear();
            // 更新，因为之前操作可能会注册新的BeanDefinitionRegistryPostProcessor
            postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
                    // 实现了Ordered接口的先实例化，并添加到currentRegistryProcessors列表
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    // 名字加到processedBeans列表
                    processedBeans.add(ppName);
                }
            }
            // 排序
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            // 添加currentRegistryProcessors到registryProcessors
            registryProcessors.addAll(currentRegistryProcessors);
            // 遍历currentRegistryProcessors列表，执行postProcessBeanDefinitionRegistry
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            // 清空currentRegistryProcessors
            currentRegistryProcessors.clear();
            // 执行剩下的BeanDefinitionRegistryPostProcessor
            boolean reiterate = true;
            while (reiterate) {
                reiterate = false;
                // 更新
                postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
                // 遍历
                for (String ppName : postProcessorNames) {
                    // 如果之前没有处理过
                    if (!processedBeans.contains(ppName)) {
                        // 实例化，并添加到currentRegistryProcessors列表
                        currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                        // 名字加到processedBeans列表
                        processedBeans.add(ppName);
                        // 需要继续执行
                        reiterate = true;
                    }
                }
                // 排序
                sortPostProcessors(currentRegistryProcessors, beanFactory);
                // 添加currentRegistryProcessors到registryProcessors
                registryProcessors.addAll(currentRegistryProcessors);
                // 遍历currentRegistryProcessors列表，执行postProcessBeanDefinitionRegistry
                invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
                // 清空currentRegistryProcessors
                currentRegistryProcessors.clear();
            }
            // 遍历registryProcessors列表，执行postProcessBeanFactory
            invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
            // 遍历regularPostProcessors列表，执行postProcessBeanFactory
            invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
        } else {
            // Invoke factory processors registered with the context instance.
            invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
        }
        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let the bean factory post-processors apply to them!
        // 获取已注册的BeanFactoryPostProcessor的beanName
        String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);
        // Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
        // Ordered, and the rest.
        List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
        List<String> orderedPostProcessorNames = new ArrayList<>();
        List<String> nonOrderedPostProcessorNames = new ArrayList<>();
        // 按上面的逻辑
        for (String ppName : postProcessorNames) {
            if (processedBeans.contains(ppName)) {
                // skip - already processed in first phase above
            } else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
            } else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
                orderedPostProcessorNames.add(ppName);
            } else {
                nonOrderedPostProcessorNames.add(ppName);
            }
        }
        // First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
        sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
        invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);
        // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
        List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
        for (String postProcessorName : orderedPostProcessorNames) {
            orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
        }
        sortPostProcessors(orderedPostProcessors, beanFactory);
        invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);
        // Finally, invoke all other BeanFactoryPostProcessors.
        List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
        for (String postProcessorName : nonOrderedPostProcessorNames) {
            nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
        }
        invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);
        // Clear cached merged bean definitions since the post-processors might have
        // modified the original metadata, e.g. replacing placeholders in values...
        beanFactory.clearMetadataCache();
    }

    public static void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
        // 获取BeanPostProcessor名字
        String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
        // Register BeanPostProcessorChecker that logs an info message when
        // a bean is created during BeanPostProcessor instantiation, i.e. when
        // a bean is not eligible for getting processed by all BeanPostProcessors.
        int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
        beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));
        // 根据实现BeanPostProcessor接口的不同放入不同的列表里
        List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
        List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
        List<String> orderedPostProcessorNames = new ArrayList<>();
        List<String> nonOrderedPostProcessorNames = new ArrayList<>();
        // 遍历
        for (String ppName : postProcessorNames) {
            // 优先有序 PriorityOrdered
            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                // 优先实例化Bean
                BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
                // 添加
                priorityOrderedPostProcessors.add(pp);
                // MergedBeanDefinitionPostProcessor
                if (pp instanceof MergedBeanDefinitionPostProcessor) {
                    // 添加
                    internalPostProcessors.add(pp);
                }
            }
            // 有序 Ordered
            else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
                // 添加
                orderedPostProcessorNames.add(ppName);
            }
            // 其他
            else {
                // 添加
                nonOrderedPostProcessorNames.add(ppName);
            }
        }
        // 排序
        sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
        // 添加PriorityOrdered的BeanPostProcessors实例到BeanFactory的beanPostProcessors
        registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);
        // 轮到Ordered了
        List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
        for (String ppName : orderedPostProcessorNames) {
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            orderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        sortPostProcessors(orderedPostProcessors, beanFactory);
        registerBeanPostProcessors(beanFactory, orderedPostProcessors);
        // 剩下的是其他的
        List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
        for (String ppName : nonOrderedPostProcessorNames) {
            BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
            nonOrderedPostProcessors.add(pp);
            if (pp instanceof MergedBeanDefinitionPostProcessor) {
                internalPostProcessors.add(pp);
            }
        }
        registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);
        // 最后排序
        sortPostProcessors(internalPostProcessors, beanFactory);
        // 再添加一次，覆盖旧的
        registerBeanPostProcessors(beanFactory, internalPostProcessors);
        // 覆盖旧的ApplicationListenerDetector，放到链的最后
        beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
    }

    private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
        Comparator<Object> comparatorToUse = null;
        if (beanFactory instanceof DefaultListableBeanFactory) {
            comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
        }
        if (comparatorToUse == null) {
            comparatorToUse = OrderComparator.INSTANCE;
        }
        postProcessors.sort(comparatorToUse);
    }

    private static void invokeBeanDefinitionRegistryPostProcessors(Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {
        for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessBeanDefinitionRegistry(registry);
        }
    }

    private static void invokeBeanFactoryPostProcessors(Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {
        for (BeanFactoryPostProcessor postProcessor : postProcessors) {
            postProcessor.postProcessBeanFactory(beanFactory);
        }
    }

    private static void registerBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {
        for (BeanPostProcessor postProcessor : postProcessors) {
            beanFactory.addBeanPostProcessor(postProcessor);
        }
    }

    private static final class BeanPostProcessorChecker implements BeanPostProcessor {

        private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

        private final ConfigurableListableBeanFactory beanFactory;

        private final int beanPostProcessorTargetCount;

        public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
            this.beanFactory = beanFactory;
            this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
        }

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) && this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
                if (logger.isInfoEnabled()) {
                    logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() + "] is not eligible for getting processed by all BeanPostProcessors " + "(for example: not eligible for auto-proxying)");
                }
            }
            return bean;
        }

        private boolean isInfrastructureBean(@Nullable String beanName) {
            if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
                BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
                return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
            }
            return false;
        }

    }

}
