package org.springframework.aop.framework;

import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.Ordered;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.io.Closeable;

@SuppressWarnings("serial")
public class ProxyProcessorSupport extends ProxyConfig implements Ordered, BeanClassLoaderAware, AopInfrastructureBean {

    private int order = Ordered.LOWEST_PRECEDENCE;

    @Nullable
    private ClassLoader proxyClassLoader = ClassUtils.getDefaultClassLoader();

    private boolean classLoaderConfigured = false;

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public void setProxyClassLoader(@Nullable ClassLoader classLoader) {
        this.proxyClassLoader = classLoader;
        this.classLoaderConfigured = (classLoader != null);
    }

    @Nullable
    protected ClassLoader getProxyClassLoader() {
        return this.proxyClassLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        if (!this.classLoaderConfigured) {
            this.proxyClassLoader = classLoader;
        }
    }

    protected void evaluateProxyInterfaces(Class<?> beanClass, ProxyFactory proxyFactory) {
        Class<?>[] targetInterfaces = ClassUtils.getAllInterfacesForClass(beanClass, getProxyClassLoader());
        boolean hasReasonableProxyInterface = false;
        // 遍历接口
        for (Class<?> ifc : targetInterfaces) {
            // 不是某些配置回调接口 && 不是内置语言接口 && 接口有方法
            if (!isConfigurationCallbackInterface(ifc) && !isInternalLanguageInterface(ifc) && ifc.getMethods().length > 0) {
                hasReasonableProxyInterface = true;
                break;
            }
        }
        if (hasReasonableProxyInterface) {
            for (Class<?> ifc : targetInterfaces) {
                // 添加
                proxyFactory.addInterface(ifc);
            }
        } else {
            proxyFactory.setProxyTargetClass(true);
        }
    }

    protected boolean isConfigurationCallbackInterface(Class<?> ifc) {
        return (InitializingBean.class == ifc || DisposableBean.class == ifc || Closeable.class == ifc || AutoCloseable.class == ifc || ObjectUtils.containsElement(ifc.getInterfaces(), Aware.class));
    }

    protected boolean isInternalLanguageInterface(Class<?> ifc) {
        return (ifc.getName().equals("groovy.lang.GroovyObject") || ifc.getName().endsWith(".cglib.proxy.Factory") || ifc.getName().endsWith(".bytebuddy.MockAccess"));
    }

}
