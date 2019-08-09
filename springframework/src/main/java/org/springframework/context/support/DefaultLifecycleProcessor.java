package org.springframework.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DefaultLifecycleProcessor implements LifecycleProcessor, BeanFactoryAware {

    private final Log logger = LogFactory.getLog(getClass());

    private volatile long timeoutPerShutdownPhase = 30000;

    private volatile boolean running;

    @Nullable
    private volatile ConfigurableListableBeanFactory beanFactory;

    public void setTimeoutPerShutdownPhase(long timeoutPerShutdownPhase) {
        this.timeoutPerShutdownPhase = timeoutPerShutdownPhase;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException("DefaultLifecycleProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }

    private ConfigurableListableBeanFactory getBeanFactory() {
        ConfigurableListableBeanFactory beanFactory = this.beanFactory;
        Assert.state(beanFactory != null, "No BeanFactory available");
        return beanFactory;
    }
    // Lifecycle implementation

    @Override
    public void start() {
        startBeans(false);
        this.running = true;
    }

    @Override
    public void stop() {
        stopBeans();
        this.running = false;
    }

    @Override
    public void onRefresh() {
        startBeans(true);
        this.running = true;
    }

    @Override
    public void onClose() {
        stopBeans();
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }
    // Internal helpers

    private void startBeans(boolean autoStartupOnly) {
        Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
        Map<Integer, LifecycleGroup> phases = new HashMap<>();
        lifecycleBeans.forEach((beanName, bean) -> {
            if (!autoStartupOnly || (bean instanceof SmartLifecycle && ((SmartLifecycle) bean).isAutoStartup())) {
                int phase = getPhase(bean);
                LifecycleGroup group = phases.get(phase);
                if (group == null) {
                    group = new LifecycleGroup(phase, this.timeoutPerShutdownPhase, lifecycleBeans, autoStartupOnly);
                    phases.put(phase, group);
                }
                group.add(beanName, bean);
            }
        });
        if (!phases.isEmpty()) {
            List<Integer> keys = new ArrayList<>(phases.keySet());
            Collections.sort(keys);
            for (Integer key : keys) {
                phases.get(key).start();
            }
        }
    }

    private void doStart(Map<String, ? extends Lifecycle> lifecycleBeans, String beanName, boolean autoStartupOnly) {
        Lifecycle bean = lifecycleBeans.remove(beanName);
        if (bean != null && bean != this) {
            String[] dependenciesForBean = getBeanFactory().getDependenciesForBean(beanName);
            for (String dependency : dependenciesForBean) {
                doStart(lifecycleBeans, dependency, autoStartupOnly);
            }
            if (!bean.isRunning() && (!autoStartupOnly || !(bean instanceof SmartLifecycle) || ((SmartLifecycle) bean).isAutoStartup())) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Starting bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
                }
                try {
                    bean.start();
                } catch (Throwable ex) {
                    throw new ApplicationContextException("Failed to start bean '" + beanName + "'", ex);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Successfully started bean '" + beanName + "'");
                }
            }
        }
    }

    private void stopBeans() {
        Map<String, Lifecycle> lifecycleBeans = getLifecycleBeans();
        Map<Integer, LifecycleGroup> phases = new HashMap<>();
        lifecycleBeans.forEach((beanName, bean) -> {
            int shutdownPhase = getPhase(bean);
            LifecycleGroup group = phases.get(shutdownPhase);
            if (group == null) {
                group = new LifecycleGroup(shutdownPhase, this.timeoutPerShutdownPhase, lifecycleBeans, false);
                phases.put(shutdownPhase, group);
            }
            group.add(beanName, bean);
        });
        if (!phases.isEmpty()) {
            List<Integer> keys = new ArrayList<>(phases.keySet());
            keys.sort(Collections.reverseOrder());
            for (Integer key : keys) {
                phases.get(key).stop();
            }
        }
    }

    private void doStop(Map<String, ? extends Lifecycle> lifecycleBeans, final String beanName, final CountDownLatch latch, final Set<String> countDownBeanNames) {
        Lifecycle bean = lifecycleBeans.remove(beanName);
        if (bean != null) {
            String[] dependentBeans = getBeanFactory().getDependentBeans(beanName);
            for (String dependentBean : dependentBeans) {
                doStop(lifecycleBeans, dependentBean, latch, countDownBeanNames);
            }
            try {
                if (bean.isRunning()) {
                    if (bean instanceof SmartLifecycle) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Asking bean '" + beanName + "' of type [" + bean.getClass().getName() + "] to stop");
                        }
                        countDownBeanNames.add(beanName);
                        ((SmartLifecycle) bean).stop(() -> {
                            latch.countDown();
                            countDownBeanNames.remove(beanName);
                            if (logger.isDebugEnabled()) {
                                logger.debug("Bean '" + beanName + "' completed its stop procedure");
                            }
                        });
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Stopping bean '" + beanName + "' of type [" + bean.getClass().getName() + "]");
                        }
                        bean.stop();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Successfully stopped bean '" + beanName + "'");
                        }
                    }
                } else if (bean instanceof SmartLifecycle) {
                    // Don't wait for beans that aren't running...
                    latch.countDown();
                }
            } catch (Throwable ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to stop bean '" + beanName + "'", ex);
                }
            }
        }
    }
    // overridable hooks

    protected Map<String, Lifecycle> getLifecycleBeans() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        Map<String, Lifecycle> beans = new LinkedHashMap<>();
        String[] beanNames = beanFactory.getBeanNamesForType(Lifecycle.class, false, false);
        for (String beanName : beanNames) {
            String beanNameToRegister = BeanFactoryUtils.transformedBeanName(beanName);
            boolean isFactoryBean = beanFactory.isFactoryBean(beanNameToRegister);
            String beanNameToCheck = (isFactoryBean ? BeanFactory.FACTORY_BEAN_PREFIX + beanName : beanName);
            if ((beanFactory.containsSingleton(beanNameToRegister) && (!isFactoryBean || matchesBeanType(Lifecycle.class, beanNameToCheck, beanFactory))) || matchesBeanType(SmartLifecycle.class, beanNameToCheck, beanFactory)) {
                Object bean = beanFactory.getBean(beanNameToCheck);
                if (bean != this && bean instanceof Lifecycle) {
                    beans.put(beanNameToRegister, (Lifecycle) bean);
                }
            }
        }
        return beans;
    }

    private boolean matchesBeanType(Class<?> targetType, String beanName, BeanFactory beanFactory) {
        Class<?> beanType = beanFactory.getType(beanName);
        return (beanType != null && targetType.isAssignableFrom(beanType));
    }

    protected int getPhase(Lifecycle bean) {
        return (bean instanceof Phased ? ((Phased) bean).getPhase() : 0);
    }

    private class LifecycleGroup {

        private final int phase;

        private final long timeout;

        private final Map<String, ? extends Lifecycle> lifecycleBeans;

        private final boolean autoStartupOnly;

        private final List<LifecycleGroupMember> members = new ArrayList<>();

        private int smartMemberCount;

        public LifecycleGroup(int phase, long timeout, Map<String, ? extends Lifecycle> lifecycleBeans, boolean autoStartupOnly) {
            this.phase = phase;
            this.timeout = timeout;
            this.lifecycleBeans = lifecycleBeans;
            this.autoStartupOnly = autoStartupOnly;
        }

        public void add(String name, Lifecycle bean) {
            this.members.add(new LifecycleGroupMember(name, bean));
            if (bean instanceof SmartLifecycle) {
                this.smartMemberCount++;
            }
        }

        public void start() {
            if (this.members.isEmpty()) {
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Starting beans in phase " + this.phase);
            }
            Collections.sort(this.members);
            for (LifecycleGroupMember member : this.members) {
                doStart(this.lifecycleBeans, member.name, this.autoStartupOnly);
            }
        }

        public void stop() {
            if (this.members.isEmpty()) {
                return;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Stopping beans in phase " + this.phase);
            }
            this.members.sort(Collections.reverseOrder());
            CountDownLatch latch = new CountDownLatch(this.smartMemberCount);
            Set<String> countDownBeanNames = Collections.synchronizedSet(new LinkedHashSet<>());
            Set<String> lifecycleBeanNames = new HashSet<>(this.lifecycleBeans.keySet());
            for (LifecycleGroupMember member : this.members) {
                if (lifecycleBeanNames.contains(member.name)) {
                    doStop(this.lifecycleBeans, member.name, latch, countDownBeanNames);
                } else if (member.bean instanceof SmartLifecycle) {
                    // Already removed: must have been a dependent bean from another phase
                    latch.countDown();
                }
            }
            try {
                latch.await(this.timeout, TimeUnit.MILLISECONDS);
                if (latch.getCount() > 0 && !countDownBeanNames.isEmpty() && logger.isInfoEnabled()) {
                    logger.info("Failed to shut down " + countDownBeanNames.size() + " bean" + (countDownBeanNames.size() > 1 ? "s" : "") + " with phase value " + this.phase + " within timeout of " + this.timeout + ": " + countDownBeanNames);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }

    private class LifecycleGroupMember implements Comparable<LifecycleGroupMember> {

        private final String name;

        private final Lifecycle bean;

        LifecycleGroupMember(String name, Lifecycle bean) {
            this.name = name;
            this.bean = bean;
        }

        @Override
        public int compareTo(LifecycleGroupMember other) {
            int thisPhase = getPhase(this.bean);
            int otherPhase = getPhase(other.bean);
            return Integer.compare(thisPhase, otherPhase);
        }

    }

}
