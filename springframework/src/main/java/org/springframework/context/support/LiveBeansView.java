package org.springframework.context.support;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class LiveBeansView implements LiveBeansViewMBean, ApplicationContextAware {

    public static final String MBEAN_DOMAIN_PROPERTY_NAME = "spring.liveBeansView.mbeanDomain";

    public static final String MBEAN_APPLICATION_KEY = "application";

    private static final Set<ConfigurableApplicationContext> applicationContexts = new LinkedHashSet<>();

    @Nullable
    private static String applicationName;

    static void registerApplicationContext(ConfigurableApplicationContext applicationContext) {
        String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
        if (mbeanDomain != null) {
            synchronized (applicationContexts) {
                if (applicationContexts.isEmpty()) {
                    try {
                        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                        applicationName = applicationContext.getApplicationName();
                        server.registerMBean(new LiveBeansView(), new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
                    } catch (Throwable ex) {
                        throw new ApplicationContextException("Failed to register LiveBeansView MBean", ex);
                    }
                }
                applicationContexts.add(applicationContext);
            }
        }
    }

    static void unregisterApplicationContext(ConfigurableApplicationContext applicationContext) {
        synchronized (applicationContexts) {
            if (applicationContexts.remove(applicationContext) && applicationContexts.isEmpty()) {
                try {
                    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                    String mbeanDomain = applicationContext.getEnvironment().getProperty(MBEAN_DOMAIN_PROPERTY_NAME);
                    if (mbeanDomain != null) {
                        server.unregisterMBean(new ObjectName(mbeanDomain, MBEAN_APPLICATION_KEY, applicationName));
                    }
                } catch (Throwable ex) {
                    throw new ApplicationContextException("Failed to unregister LiveBeansView MBean", ex);
                } finally {
                    applicationName = null;
                }
            }
        }
    }

    @Nullable
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        Assert.isTrue(applicationContext instanceof ConfigurableApplicationContext, "ApplicationContext does not implement ConfigurableApplicationContext");
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    @Override
    public String getSnapshotAsJson() {
        Set<ConfigurableApplicationContext> contexts;
        if (this.applicationContext != null) {
            contexts = Collections.singleton(this.applicationContext);
        } else {
            contexts = findApplicationContexts();
        }
        return generateJson(contexts);
    }

    protected Set<ConfigurableApplicationContext> findApplicationContexts() {
        synchronized (applicationContexts) {
            return new LinkedHashSet<>(applicationContexts);
        }
    }

    protected String generateJson(Set<ConfigurableApplicationContext> contexts) {
        StringBuilder result = new StringBuilder("[\n");
        for (Iterator<ConfigurableApplicationContext> it = contexts.iterator(); it.hasNext(); ) {
            ConfigurableApplicationContext context = it.next();
            result.append("{\n\"context\": \"").append(context.getId()).append("\",\n");
            if (context.getParent() != null) {
                result.append("\"parent\": \"").append(context.getParent().getId()).append("\",\n");
            } else {
                result.append("\"parent\": null,\n");
            }
            result.append("\"beans\": [\n");
            ConfigurableListableBeanFactory bf = context.getBeanFactory();
            String[] beanNames = bf.getBeanDefinitionNames();
            boolean elementAppended = false;
            for (String beanName : beanNames) {
                BeanDefinition bd = bf.getBeanDefinition(beanName);
                if (isBeanEligible(beanName, bd, bf)) {
                    if (elementAppended) {
                        result.append(",\n");
                    }
                    result.append("{\n\"bean\": \"").append(beanName).append("\",\n");
                    result.append("\"aliases\": ");
                    appendArray(result, bf.getAliases(beanName));
                    result.append(",\n");
                    String scope = bd.getScope();
                    if (!StringUtils.hasText(scope)) {
                        scope = BeanDefinition.SCOPE_SINGLETON;
                    }
                    result.append("\"scope\": \"").append(scope).append("\",\n");
                    Class<?> beanType = bf.getType(beanName);
                    if (beanType != null) {
                        result.append("\"type\": \"").append(beanType.getName()).append("\",\n");
                    } else {
                        result.append("\"type\": null,\n");
                    }
                    result.append("\"resource\": \"").append(getEscapedResourceDescription(bd)).append("\",\n");
                    result.append("\"dependencies\": ");
                    appendArray(result, bf.getDependenciesForBean(beanName));
                    result.append("\n}");
                    elementAppended = true;
                }
            }
            result.append("]\n");
            result.append("}");
            if (it.hasNext()) {
                result.append(",\n");
            }
        }
        result.append("]");
        return result.toString();
    }

    protected boolean isBeanEligible(String beanName, BeanDefinition bd, ConfigurableBeanFactory bf) {
        return (bd.getRole() != BeanDefinition.ROLE_INFRASTRUCTURE && (!bd.isLazyInit() || bf.containsSingleton(beanName)));
    }

    @Nullable
    protected String getEscapedResourceDescription(BeanDefinition bd) {
        String resourceDescription = bd.getResourceDescription();
        if (resourceDescription == null) {
            return null;
        }
        StringBuilder result = new StringBuilder(resourceDescription.length() + 16);
        for (int i = 0; i < resourceDescription.length(); i++) {
            char character = resourceDescription.charAt(i);
            if (character == '\\') {
                result.append('/');
            } else if (character == '"') {
                result.append("\\").append('"');
            } else {
                result.append(character);
            }
        }
        return result.toString();
    }

    private void appendArray(StringBuilder result, String[] arr) {
        result.append('[');
        if (arr.length > 0) {
            result.append('\"');
        }
        result.append(StringUtils.arrayToDelimitedString(arr, "\", \""));
        if (arr.length > 0) {
            result.append('\"');
        }
        result.append(']');
    }

}
