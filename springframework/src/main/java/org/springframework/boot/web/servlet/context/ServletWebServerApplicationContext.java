package org.springframework.boot.web.servlet.context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.web.context.ConfigurableWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletContextInitializerBeans;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.context.support.ServletContextResource;
import org.springframework.web.context.support.ServletContextScope;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.*;

public class ServletWebServerApplicationContext extends GenericWebApplicationContext implements ConfigurableWebServerApplicationContext {

    private static final Log logger = LogFactory.getLog(ServletWebServerApplicationContext.class);

    public static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

    private volatile WebServer webServer;

    private ServletConfig servletConfig;

    private String serverNamespace;

    public ServletWebServerApplicationContext() {
    }

    public ServletWebServerApplicationContext(DefaultListableBeanFactory beanFactory) {
        super(beanFactory);
    }

    @Override
    protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        /*
         * 添加WebApplicationContextServletContextAwareProcessor
         * 用于向实现了ServletContextAware的Bean通过ConfigurableWebApplicationContext注入ServletContext
         */
        beanFactory.addBeanPostProcessor(new WebApplicationContextServletContextAwareProcessor(this));
        beanFactory.ignoreDependencyInterface(ServletContextAware.class);
        // 向BeanFactory注册了Servlet相关的依赖
        registerWebApplicationScopes();
    }

    @Override
    public final void refresh() throws BeansException, IllegalStateException {
        try {
            super.refresh();
        } catch (RuntimeException ex) {
            stopAndReleaseWebServer();
            throw ex;
        }
    }

    @Override
    protected void onRefresh() {
        super.onRefresh();
        try {
            createWebServer();
        } catch (Throwable ex) {
            throw new ApplicationContextException("Unable to start web server", ex);
        }
    }

    @Override
    protected void finishRefresh() {
        super.finishRefresh();
        WebServer webServer = startWebServer();
        if (webServer != null) {
            publishEvent(new ServletWebServerInitializedEvent(webServer, this));
        }
    }

    @Override
    protected void onClose() {
        super.onClose();
        stopAndReleaseWebServer();
    }

    private void createWebServer() {
        WebServer webServer = this.webServer;
        ServletContext servletContext = getServletContext();
        // 一般情况这里没初始化都是null
        if (webServer == null && servletContext == null) {
            // 实例化TomcatServletWebServerFactory，之前在ServletWebServerFactoryAutoConfiguration导入
            ServletWebServerFactory factory = getWebServerFactory();
            /*
             * 生成ServletContextInitializer的匿名实现，主要处理：
             *      将WebApplicationContext和ServletContext互相添加到对方的属性
             *      从BeanFactory实例化ServletContextInitializer类型的Bean，这里是之前在DispatcherServletAutoConfiguration导入的DispatcherServletRegistrationBean，并加入加入initializers集合
             *      从BeanFactory实例化Servlet类型的Bean，通过RegistrationBeanAdapter生成对应的RegistrationBean，并将它加入initializers集合
             *      从BeanFactory实例化Filter类型的Bean，通过RegistrationBeanAdapter生成对应的RegistrationBean，并将它加入initializers集合
             *      从BeanFactory实例化各种EventListener类型的Bean，通过RegistrationBeanAdapter生成对应的RegistrationBean，并将它加入initializers集合
             *      排序后遍历调用ServletContextInitializer#onStartup
             * 通过ServletWebServerFactory#getWebServer实例化Tomcat
             * 实例化一个TomcatStarter，TomcatStarter实现了ServletContainerInitializer接口，并将之前生成的ServletContextInitializer的匿名实现传入
             * 将TomcatStarter实例加入Tomcat的Context
             * 启动Tomcat并使其阻塞（与Jetty不同，Tomcat所有的线程都是守护线程，所以创建一个阻塞的非守护线程避免立即shutdown）
             * Tomcat启动后会调用ServletContainerInitializer#onStartup，执行之前传入的ServletContextInitializer的匿名实现的onStartup方法
             */
            this.webServer = factory.getWebServer(getSelfInitializer());
        } else if (servletContext != null) {
            try {
                getSelfInitializer().onStartup(servletContext);
            } catch (ServletException ex) {
                throw new ApplicationContextException("Cannot initialize servlet context", ex);
            }
        }
        initPropertySources();
    }

    protected ServletWebServerFactory getWebServerFactory() {
        // Use bean names so that we don't consider the hierarchy
        String[] beanNames = getBeanFactory().getBeanNamesForType(ServletWebServerFactory.class);
        if (beanNames.length == 0) {
            throw new ApplicationContextException("Unable to start ServletWebServerApplicationContext due to missing " + "ServletWebServerFactory bean.");
        }
        if (beanNames.length > 1) {
            throw new ApplicationContextException("Unable to start ServletWebServerApplicationContext due to multiple " + "ServletWebServerFactory beans : " + StringUtils.arrayToCommaDelimitedString(beanNames));
        }
        return getBeanFactory().getBean(beanNames[0], ServletWebServerFactory.class);
    }

    private org.springframework.boot.web.servlet.ServletContextInitializer getSelfInitializer() {
        // 返回一个FunctionalInterface实现
        return this::selfInitialize;
    }

    private void selfInitialize(ServletContext servletContext) throws ServletException {
        //
        prepareWebApplicationContext(servletContext);
        registerApplicationScope(servletContext);
        WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(), servletContext);
        // 复杂操作，触发Servlet, Filter和EventListener注入到ServletContext
        for (ServletContextInitializer beans : getServletContextInitializerBeans()) {
            // onStartup
            beans.onStartup(servletContext);
        }
    }

    private void registerApplicationScope(ServletContext servletContext) {
        ServletContextScope appScope = new ServletContextScope(servletContext);
        getBeanFactory().registerScope(WebApplicationContext.SCOPE_APPLICATION, appScope);
        // Register as ServletContext attribute, for ContextCleanupListener to detect it.
        servletContext.setAttribute(ServletContextScope.class.getName(), appScope);
    }

    private void registerWebApplicationScopes() {
        ExistingWebApplicationScopes existingScopes = new ExistingWebApplicationScopes(getBeanFactory());
        WebApplicationContextUtils.registerWebApplicationScopes(getBeanFactory());
        existingScopes.restore();
    }

    protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {
        return new ServletContextInitializerBeans(getBeanFactory());
    }

    protected void prepareWebApplicationContext(ServletContext servletContext) {
        Object rootContext = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (rootContext != null) {
            if (rootContext == this) {
                throw new IllegalStateException("Cannot initialize context because there is already a root application context present - " + "check whether you have multiple ServletContextInitializers!");
            }
            return;
        }
        Log logger = LogFactory.getLog(ContextLoader.class);
        servletContext.log("Initializing Spring embedded WebApplicationContext");
        try {
            // 将WebApplicationContext加到ServletContext
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this);
            if (logger.isDebugEnabled()) {
                logger.debug("Published root WebApplicationContext as ServletContext attribute with name [" + WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE + "]");
            }
            // 将ServletContext加到WebApplicationContext
            setServletContext(servletContext);
            if (logger.isInfoEnabled()) {
                long elapsedTime = System.currentTimeMillis() - getStartupDate();
                logger.info("Root WebApplicationContext: initialization completed in " + elapsedTime + " ms");
            }
        } catch (RuntimeException | Error ex) {
            logger.error("Context initialization failed", ex);
            servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, ex);
            throw ex;
        }
    }

    private WebServer startWebServer() {
        WebServer webServer = this.webServer;
        if (webServer != null) {
            webServer.start();
        }
        return webServer;
    }

    private void stopAndReleaseWebServer() {
        WebServer webServer = this.webServer;
        if (webServer != null) {
            try {
                webServer.stop();
                this.webServer = null;
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    @Override
    protected Resource getResourceByPath(String path) {
        if (getServletContext() == null) {
            return new ClassPathContextResource(path, getClassLoader());
        }
        return new ServletContextResource(getServletContext(), path);
    }

    @Override
    public String getServerNamespace() {
        return this.serverNamespace;
    }

    @Override
    public void setServerNamespace(String serverNamespace) {
        this.serverNamespace = serverNamespace;
    }

    @Override
    public void setServletConfig(ServletConfig servletConfig) {
        this.servletConfig = servletConfig;
    }

    @Override
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    @Override
    public WebServer getWebServer() {
        return this.webServer;
    }

    public static class ExistingWebApplicationScopes {

        private static final Set<String> SCOPES;

        static {
            Set<String> scopes = new LinkedHashSet<>();
            scopes.add(WebApplicationContext.SCOPE_REQUEST);
            scopes.add(WebApplicationContext.SCOPE_SESSION);
            SCOPES = Collections.unmodifiableSet(scopes);
        }

        private final ConfigurableListableBeanFactory beanFactory;

        private final Map<String, Scope> scopes = new HashMap<>();

        public ExistingWebApplicationScopes(ConfigurableListableBeanFactory beanFactory) {
            this.beanFactory = beanFactory;
            for (String scopeName : SCOPES) {
                Scope scope = beanFactory.getRegisteredScope(scopeName);
                if (scope != null) {
                    this.scopes.put(scopeName, scope);
                }
            }
        }

        public void restore() {
            this.scopes.forEach((key, value) -> {
                if (logger.isInfoEnabled()) {
                    logger.info("Restoring user defined scope " + key);
                }
                this.beanFactory.registerScope(key, value);
            });
        }

    }

}
