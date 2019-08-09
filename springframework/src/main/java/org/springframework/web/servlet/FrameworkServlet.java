package org.springframework.web.servlet;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SourceFilteringListener;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ConfigurableWebEnvironment;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.async.CallableProcessingInterceptor;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public abstract class FrameworkServlet extends HttpServletBean implements ApplicationContextAware {

    public static final String DEFAULT_NAMESPACE_SUFFIX = "-servlet";

    public static final Class<?> DEFAULT_CONTEXT_CLASS = XmlWebApplicationContext.class;

    public static final String SERVLET_CONTEXT_PREFIX = FrameworkServlet.class.getName() + ".CONTEXT.";

    private static final String INIT_PARAM_DELIMITERS = ",; \t\n";

    @Nullable
    private String contextAttribute;

    private Class<?> contextClass = DEFAULT_CONTEXT_CLASS;

    @Nullable
    private String contextId;

    @Nullable
    private String namespace;

    @Nullable
    private String contextConfigLocation;

    private final List<ApplicationContextInitializer<ConfigurableApplicationContext>> contextInitializers = new ArrayList<>();

    @Nullable
    private String contextInitializerClasses;

    private boolean publishContext = true;

    private boolean publishEvents = true;

    private boolean threadContextInheritable = false;

    private boolean dispatchOptionsRequest = false;

    private boolean dispatchTraceRequest = false;

    private boolean enableLoggingRequestDetails = false;

    @Nullable
    private WebApplicationContext webApplicationContext;

    private boolean webApplicationContextInjected = false;

    private volatile boolean refreshEventReceived = false;

    private final Object onRefreshMonitor = new Object();

    public FrameworkServlet() {
    }

    public FrameworkServlet(WebApplicationContext webApplicationContext) {
        this.webApplicationContext = webApplicationContext;
    }

    public void setContextAttribute(@Nullable String contextAttribute) {
        this.contextAttribute = contextAttribute;
    }

    @Nullable
    public String getContextAttribute() {
        return this.contextAttribute;
    }

    public void setContextClass(Class<?> contextClass) {
        this.contextClass = contextClass;
    }

    public Class<?> getContextClass() {
        return this.contextClass;
    }

    public void setContextId(@Nullable String contextId) {
        this.contextId = contextId;
    }

    @Nullable
    public String getContextId() {
        return this.contextId;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getNamespace() {
        return (this.namespace != null ? this.namespace : getServletName() + DEFAULT_NAMESPACE_SUFFIX);
    }

    public void setContextConfigLocation(@Nullable String contextConfigLocation) {
        this.contextConfigLocation = contextConfigLocation;
    }

    @Nullable
    public String getContextConfigLocation() {
        return this.contextConfigLocation;
    }

    @SuppressWarnings("unchecked")
    public void setContextInitializers(@Nullable ApplicationContextInitializer<?>... initializers) {
        if (initializers != null) {
            for (ApplicationContextInitializer<?> initializer : initializers) {
                this.contextInitializers.add((ApplicationContextInitializer<ConfigurableApplicationContext>) initializer);
            }
        }
    }

    public void setContextInitializerClasses(String contextInitializerClasses) {
        this.contextInitializerClasses = contextInitializerClasses;
    }

    public void setPublishContext(boolean publishContext) {
        this.publishContext = publishContext;
    }

    public void setPublishEvents(boolean publishEvents) {
        this.publishEvents = publishEvents;
    }

    public void setThreadContextInheritable(boolean threadContextInheritable) {
        this.threadContextInheritable = threadContextInheritable;
    }

    public void setDispatchOptionsRequest(boolean dispatchOptionsRequest) {
        this.dispatchOptionsRequest = dispatchOptionsRequest;
    }

    public void setDispatchTraceRequest(boolean dispatchTraceRequest) {
        this.dispatchTraceRequest = dispatchTraceRequest;
    }

    public void setEnableLoggingRequestDetails(boolean enable) {
        this.enableLoggingRequestDetails = enable;
    }

    public boolean isEnableLoggingRequestDetails() {
        return this.enableLoggingRequestDetails;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        if (this.webApplicationContext == null && applicationContext instanceof WebApplicationContext) {
            this.webApplicationContext = (WebApplicationContext) applicationContext;
            this.webApplicationContextInjected = true;
        }
    }

    /**
     *
     */
    @Override
    protected final void initServletBean() throws ServletException {
        getServletContext().log("Initializing Spring " + getClass().getSimpleName() + " '" + getServletName() + "'");
        if (logger.isInfoEnabled()) {
            logger.info("Initializing Servlet '" + getServletName() + "'");
        }
        long startTime = System.currentTimeMillis();
        try {
            //
            this.webApplicationContext = initWebApplicationContext();
            initFrameworkServlet();
        } catch (ServletException | RuntimeException ex) {
            logger.error("Context initialization failed", ex);
            throw ex;
        }
        if (logger.isDebugEnabled()) {
            String value = this.enableLoggingRequestDetails ? "shown which may lead to unsafe logging of potentially sensitive data" : "masked to prevent unsafe logging of potentially sensitive data";
            logger.debug("enableLoggingRequestDetails='" + this.enableLoggingRequestDetails + "': request parameters and headers will be " + value);
        }
        if (logger.isInfoEnabled()) {
            logger.info("Completed initialization in " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    protected WebApplicationContext initWebApplicationContext() {
        // 获得之前设为ServletContext属性的WebApplicationContext
        WebApplicationContext rootContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
        // 当前Servlet的WebApplicationContext
        WebApplicationContext wac = null;
        if (this.webApplicationContext != null) {
            // 如果在构造的时候注入了WebApplicationContext，就用它
            // 默认DispatcherServlet实现了ApplicationContextAware，所以初始化时属性已经被设置，所以wac和rootContext是同一个对象
            wac = this.webApplicationContext;
            if (wac instanceof ConfigurableWebApplicationContext) {
                ConfigurableWebApplicationContext cwac = (ConfigurableWebApplicationContext) wac;
                // WebApplicationContext没被刷新
                if (!cwac.isActive()) {
                    // WebApplicationContext实例被注入但没有显式设置父级
                    if (cwac.getParent() == null) {
                        // 设置根WebApplicationContext为父亲
                        cwac.setParent(rootContext);
                    }
                    // 配置并刷新当前的WebApplicationContext
                    configureAndRefreshWebApplicationContext(cwac);
                }
            }
        }
        // 构造的时候没有注入WebApplicationContext
        if (wac == null) {
            // 从ServletContext通过contextAttribute查找WebApplicationContext，如果存在，则认为父级Context已经被设置并且已进行初始化比如设置Context的Id
            wac = findWebApplicationContext();
        }
        if (wac == null) {
            // 当前ServletContext没有被定义WebApplicationContext，新建一个
            wac = createWebApplicationContext(rootContext);
        }
        // 默认false，且没被修改
        if (!this.refreshEventReceived) {
            // 要不是这个WebApplicationContext不是一个ConfigurableApplicationContext，不支持refresh
            // 要不就是在构造的时候被注入的WebApplicationContext已经被刷新
            // 手动触发onRefresh
            synchronized (this.onRefreshMonitor) {
                // 初始化HandlerMapping、Resolver等
                onRefresh(wac);
            }
        }
        if (this.publishContext) {
            // Publish the context as a servlet context attribute.
            String attrName = getServletContextAttributeName();
            // 将当前Servlet的WebApplicationContext添加为ServletContext的属性
            getServletContext().setAttribute(attrName, wac);
        }
        return wac;
    }

    @Nullable
    protected WebApplicationContext findWebApplicationContext() {
        String attrName = getContextAttribute();
        if (attrName == null) {
            return null;
        }
        WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(getServletContext(), attrName);
        if (wac == null) {
            throw new IllegalStateException("No WebApplicationContext found: initializer not registered?");
        }
        return wac;
    }

    protected WebApplicationContext createWebApplicationContext(@Nullable ApplicationContext parent) {
        Class<?> contextClass = getContextClass();
        // 没有继承ConfigurableWebApplicationContext
        if (!ConfigurableWebApplicationContext.class.isAssignableFrom(contextClass)) {
            throw new ApplicationContextException("Fatal initialization error in servlet with name '" + getServletName() + "': custom WebApplicationContext class [" + contextClass.getName() + "] is not of type ConfigurableWebApplicationContext");
        }
        // 默认XmlWebApplicationContext
        ConfigurableWebApplicationContext wac = (ConfigurableWebApplicationContext) BeanUtils.instantiateClass(contextClass);
        wac.setEnvironment(getEnvironment());
        wac.setParent(parent);
        String configLocation = getContextConfigLocation();
        if (configLocation != null) {
            wac.setConfigLocation(configLocation);
        }
        // 配置并刷新WebApplicationContext
        configureAndRefreshWebApplicationContext(wac);
        return wac;
    }

    protected void configureAndRefreshWebApplicationContext(ConfigurableWebApplicationContext wac) {
        if (ObjectUtils.identityToString(wac).equals(wac.getId())) {
            // The application context id is still set to its original default value
            // -> assign a more useful id based on available information
            if (this.contextId != null) {
                wac.setId(this.contextId);
            } else {
                // Generate default id...
                wac.setId(ConfigurableWebApplicationContext.APPLICATION_CONTEXT_ID_PREFIX + ObjectUtils.getDisplayString(getServletContext().getContextPath()) + '/' + getServletName());
            }
        }
        wac.setServletContext(getServletContext());
        wac.setServletConfig(getServletConfig());
        wac.setNamespace(getNamespace());
        // 注册一个监听器，收到刷新事件后调用onRefresh
        wac.addApplicationListener(new SourceFilteringListener(wac, new ContextRefreshListener()));
        // The wac environment's #initPropertySources will be called in any case when the context
        // is refreshed; do it eagerly here to ensure servlet property sources are in place for
        // use in any post-processing or initialization that occurs below prior to #refresh
        ConfigurableEnvironment env = wac.getEnvironment();
        if (env instanceof ConfigurableWebEnvironment) {
            ((ConfigurableWebEnvironment) env).initPropertySources(getServletContext(), getServletConfig());
        }
        postProcessWebApplicationContext(wac);
        applyInitializers(wac);
        // AbstractApplicationContext#refresh
        wac.refresh();
    }

    protected WebApplicationContext createWebApplicationContext(@Nullable WebApplicationContext parent) {
        return createWebApplicationContext((ApplicationContext) parent);
    }

    protected void postProcessWebApplicationContext(ConfigurableWebApplicationContext wac) {
    }

    protected void applyInitializers(ConfigurableApplicationContext wac) {
        String globalClassNames = getServletContext().getInitParameter(ContextLoader.GLOBAL_INITIALIZER_CLASSES_PARAM);
        if (globalClassNames != null) {
            for (String className : StringUtils.tokenizeToStringArray(globalClassNames, INIT_PARAM_DELIMITERS)) {
                this.contextInitializers.add(loadInitializer(className, wac));
            }
        }
        if (this.contextInitializerClasses != null) {
            for (String className : StringUtils.tokenizeToStringArray(this.contextInitializerClasses, INIT_PARAM_DELIMITERS)) {
                this.contextInitializers.add(loadInitializer(className, wac));
            }
        }
        AnnotationAwareOrderComparator.sort(this.contextInitializers);
        for (ApplicationContextInitializer<ConfigurableApplicationContext> initializer : this.contextInitializers) {
            initializer.initialize(wac);
        }
    }

    @SuppressWarnings("unchecked")
    private ApplicationContextInitializer<ConfigurableApplicationContext> loadInitializer(String className, ConfigurableApplicationContext wac) {
        try {
            Class<?> initializerClass = ClassUtils.forName(className, wac.getClassLoader());
            Class<?> initializerContextClass = GenericTypeResolver.resolveTypeArgument(initializerClass, ApplicationContextInitializer.class);
            if (initializerContextClass != null && !initializerContextClass.isInstance(wac)) {
                throw new ApplicationContextException(String.format("Could not apply context initializer [%s] since its generic parameter [%s] " + "is not assignable from the type of application context used by this " + "framework servlet: [%s]", initializerClass.getName(), initializerContextClass.getName(), wac.getClass().getName()));
            }
            return BeanUtils.instantiateClass(initializerClass, ApplicationContextInitializer.class);
        } catch (ClassNotFoundException ex) {
            throw new ApplicationContextException(String.format("Could not load class [%s] specified " + "via 'contextInitializerClasses' init-param", className), ex);
        }
    }

    public String getServletContextAttributeName() {
        return SERVLET_CONTEXT_PREFIX + getServletName();
    }

    @Nullable
    public final WebApplicationContext getWebApplicationContext() {
        return this.webApplicationContext;
    }

    protected void initFrameworkServlet() throws ServletException {
    }

    public void refresh() {
        WebApplicationContext wac = getWebApplicationContext();
        if (!(wac instanceof ConfigurableApplicationContext)) {
            throw new IllegalStateException("WebApplicationContext does not support refresh: " + wac);
        }
        ((ConfigurableApplicationContext) wac).refresh();
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        this.refreshEventReceived = true;
        synchronized (this.onRefreshMonitor) {
            onRefresh(event.getApplicationContext());
        }
    }

    protected void onRefresh(ApplicationContext context) {
        // For subclasses: do nothing by default.
    }

    @Override
    public void destroy() {
        getServletContext().log("Destroying Spring FrameworkServlet '" + getServletName() + "'");
        // Only call close() on WebApplicationContext if locally managed...
        if (this.webApplicationContext instanceof ConfigurableApplicationContext && !this.webApplicationContextInjected) {
            ((ConfigurableApplicationContext) this.webApplicationContext).close();
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpMethod httpMethod = HttpMethod.resolve(request.getMethod());
        if (httpMethod == HttpMethod.PATCH || httpMethod == null) {
            processRequest(request, response);
        } else {
            super.service(request, response);
        }
    }

    @Override
    protected final void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected final void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected final void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected final void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.dispatchOptionsRequest || CorsUtils.isPreFlightRequest(request)) {
            processRequest(request, response);
            if (response.containsHeader("Allow")) {
                // Proper OPTIONS response coming from a handler - we're done.
                return;
            }
        }
        // Use response wrapper in order to always add PATCH to the allowed methods
        super.doOptions(request, new HttpServletResponseWrapper(response) {
            @Override
            public void setHeader(String name, String value) {
                if ("Allow".equals(name)) {
                    value = (StringUtils.hasLength(value) ? value + ", " : "") + HttpMethod.PATCH.name();
                }
                super.setHeader(name, value);
            }
        });
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (this.dispatchTraceRequest) {
            processRequest(request, response);
            if ("message/http".equals(response.getContentType())) {
                // Proper TRACE response coming from a handler - we're done.
                return;
            }
        }
        super.doTrace(request, response);
    }



    /**
     * Servlet是单例多线程的，之前一直以为是一个请求创建一个新线程，现在看来Tomcat应该有复用的线程池？
     */
    protected final void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        Throwable failureCause = null;
        // 从ThreadLocal取出上一个请求的LocaleContext
        LocaleContext previousLocaleContext = LocaleContextHolder.getLocaleContext();
        // 为当前请求创建一个LocaleContext
        LocaleContext localeContext = buildLocaleContext(request);
        // 用RequestContextHolder从当前请求处理线程ThreadLocal中获取上一个请求的RequestAttributes
        RequestAttributes previousAttributes = RequestContextHolder.getRequestAttributes();
        // 基本上是每次都创建新的RequestAttributes
        ServletRequestAttributes requestAttributes = buildRequestAttributes(request, response, previousAttributes);
        // 每个请求创建一个WebAsyncManager实例，添加到属性，WebAsyncManager有一个静态变量的共享线程池
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        // 请求绑定拦截器？
        asyncManager.registerCallableInterceptor(FrameworkServlet.class.getName(), new RequestBindingInterceptor());
        // 将localeContext和requestAttributes设到当前线程的ThreadLocal，后续可以通过@Autowired的方式注入RequestAttributes
        initContextHolders(request, localeContext, requestAttributes);
        try {
            doService(request, response);
        } catch (ServletException | IOException ex) {
            failureCause = ex;
            throw ex;
        } catch (Throwable ex) {
            failureCause = ex;
            throw new NestedServletException("Request processing failed", ex);
        } finally {
            // 虽然请求完成后就没用了，但是为什么用上一个的覆盖呢，因为这样每次都会移除ThreadLocal变量？
            resetContextHolders(request, previousLocaleContext, previousAttributes);
            if (requestAttributes != null) {
                requestAttributes.requestCompleted();
            }
            logResult(request, response, failureCause, asyncManager);
            publishRequestHandledEvent(request, response, startTime, failureCause);
        }
    }

    @Nullable
    protected LocaleContext buildLocaleContext(HttpServletRequest request) {
        return new SimpleLocaleContext(request.getLocale());
    }

    @Nullable
    protected ServletRequestAttributes buildRequestAttributes(HttpServletRequest request, @Nullable HttpServletResponse response, @Nullable RequestAttributes previousAttributes) {
        if (previousAttributes == null || previousAttributes instanceof ServletRequestAttributes) {
            return new ServletRequestAttributes(request, response);
        } else {
            return null;  // preserve the pre-bound RequestAttributes instance
        }
    }

    private void initContextHolders(HttpServletRequest request, @Nullable LocaleContext localeContext, @Nullable RequestAttributes requestAttributes) {
        if (localeContext != null) {
            LocaleContextHolder.setLocaleContext(localeContext, this.threadContextInheritable);
        }
        if (requestAttributes != null) {
            RequestContextHolder.setRequestAttributes(requestAttributes, this.threadContextInheritable);
        }
    }

    private void resetContextHolders(HttpServletRequest request, @Nullable LocaleContext prevLocaleContext, @Nullable RequestAttributes previousAttributes) {
        LocaleContextHolder.setLocaleContext(prevLocaleContext, this.threadContextInheritable);
        RequestContextHolder.setRequestAttributes(previousAttributes, this.threadContextInheritable);
    }

    private void logResult(HttpServletRequest request, HttpServletResponse response, @Nullable Throwable failureCause, WebAsyncManager asyncManager) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        String dispatchType = request.getDispatcherType().name();
        boolean initialDispatch = request.getDispatcherType().equals(DispatcherType.REQUEST);
        if (failureCause != null) {
            if (!initialDispatch) {
                // FORWARD/ERROR/ASYNC: minimal message (there should be enough context already)
                if (logger.isDebugEnabled()) {
                    logger.debug("Unresolved failure from \"" + dispatchType + "\" dispatch: " + failureCause);
                }
            } else if (logger.isTraceEnabled()) {
                logger.trace("Failed to complete request", failureCause);
            } else {
                logger.debug("Failed to complete request: " + failureCause);
            }
            return;
        }
        if (asyncManager.isConcurrentHandlingStarted()) {
            logger.debug("Exiting but response remains open for further handling");
            return;
        }
        int status = response.getStatus();
        String headers = ""; // nothing below trace
        if (logger.isTraceEnabled()) {
            Collection<String> names = response.getHeaderNames();
            if (this.enableLoggingRequestDetails) {
                headers = names.stream().map(name -> name + ":" + response.getHeaders(name)).collect(Collectors.joining(", "));
            } else {
                headers = names.isEmpty() ? "" : "masked";
            }
            headers = ", headers={" + headers + "}";
        }
        if (!initialDispatch) {
            logger.debug("Exiting from \"" + dispatchType + "\" dispatch, status " + status + headers);
        } else {
            HttpStatus httpStatus = HttpStatus.resolve(status);
            logger.debug("Completed " + (httpStatus != null ? httpStatus : status) + headers);
        }
    }

    private void publishRequestHandledEvent(HttpServletRequest request, HttpServletResponse response, long startTime, @Nullable Throwable failureCause) {
        if (this.publishEvents && this.webApplicationContext != null) {
            // Whether or not we succeeded, publish an event.
            long processingTime = System.currentTimeMillis() - startTime;
            this.webApplicationContext.publishEvent(new ServletRequestHandledEvent(this, request.getRequestURI(), request.getRemoteAddr(), request.getMethod(), getServletConfig().getServletName(), WebUtils.getSessionId(request), getUsernameForRequest(request), processingTime, failureCause, response.getStatus()));
        }
    }

    @Nullable
    protected String getUsernameForRequest(HttpServletRequest request) {
        Principal userPrincipal = request.getUserPrincipal();
        return (userPrincipal != null ? userPrincipal.getName() : null);
    }

    protected abstract void doService(HttpServletRequest request, HttpServletResponse response) throws Exception;

    private class ContextRefreshListener implements ApplicationListener<ContextRefreshedEvent> {

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            FrameworkServlet.this.onApplicationEvent(event);
        }

    }

    private class RequestBindingInterceptor implements CallableProcessingInterceptor {

        @Override
        public <T> void preProcess(NativeWebRequest webRequest, Callable<T> task) {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null) {
                HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
                initContextHolders(request, buildLocaleContext(request), buildRequestAttributes(request, response, null));
            }
        }

        @Override
        public <T> void postProcess(NativeWebRequest webRequest, Callable<T> task, Object concurrentResult) {
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null) {
                resetContextHolders(request, null, null);
            }
        }

    }

}
