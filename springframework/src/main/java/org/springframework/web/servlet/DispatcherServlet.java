package org.springframework.web.servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.ThemeSource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.async.WebAsyncManager;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.WebUtils;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DispatcherServlet的load-on-startup没被设置
 * 所以初始化（init方法）不是在容器启动过程中，而是在容器接收到第一个对该Servlet的请求时
 */
@SuppressWarnings("serial")
public class DispatcherServlet extends FrameworkServlet {

    public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";

    public static final String LOCALE_RESOLVER_BEAN_NAME = "localeResolver";

    public static final String THEME_RESOLVER_BEAN_NAME = "themeResolver";

    public static final String HANDLER_MAPPING_BEAN_NAME = "handlerMapping";

    public static final String HANDLER_ADAPTER_BEAN_NAME = "handlerAdapter";

    public static final String HANDLER_EXCEPTION_RESOLVER_BEAN_NAME = "handlerExceptionResolver";

    public static final String REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME = "viewNameTranslator";

    public static final String VIEW_RESOLVER_BEAN_NAME = "viewResolver";

    public static final String FLASH_MAP_MANAGER_BEAN_NAME = "flashMapManager";

    public static final String WEB_APPLICATION_CONTEXT_ATTRIBUTE = DispatcherServlet.class.getName() + ".CONTEXT";

    public static final String LOCALE_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".LOCALE_RESOLVER";

    public static final String THEME_RESOLVER_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_RESOLVER";

    public static final String THEME_SOURCE_ATTRIBUTE = DispatcherServlet.class.getName() + ".THEME_SOURCE";

    public static final String INPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".INPUT_FLASH_MAP";

    public static final String OUTPUT_FLASH_MAP_ATTRIBUTE = DispatcherServlet.class.getName() + ".OUTPUT_FLASH_MAP";

    public static final String FLASH_MAP_MANAGER_ATTRIBUTE = DispatcherServlet.class.getName() + ".FLASH_MAP_MANAGER";

    public static final String EXCEPTION_ATTRIBUTE = DispatcherServlet.class.getName() + ".EXCEPTION";

    public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

    private static final String DEFAULT_STRATEGIES_PATH = "DispatcherServlet.properties";

    private static final String DEFAULT_STRATEGIES_PREFIX = "org.springframework.web.servlet";

    protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

    private static final Properties defaultStrategies;

    static {
        // Load default strategy implementations from properties file.
        // This is currently strictly internal and not meant to be customized
        // by application developers.
        try {
            ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);
            defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());
        }
    }

    private boolean detectAllHandlerMappings = true;

    private boolean detectAllHandlerAdapters = true;

    private boolean detectAllHandlerExceptionResolvers = true;

    private boolean detectAllViewResolvers = true;

    private boolean throwExceptionIfNoHandlerFound = false;

    private boolean cleanupAfterInclude = true;

    @Nullable
    private MultipartResolver multipartResolver;

    @Nullable
    private LocaleResolver localeResolver;

    @Nullable
    private ThemeResolver themeResolver;

    @Nullable
    private List<HandlerMapping> handlerMappings;

    @Nullable
    private List<HandlerAdapter> handlerAdapters;

    @Nullable
    private List<HandlerExceptionResolver> handlerExceptionResolvers;

    @Nullable
    private RequestToViewNameTranslator viewNameTranslator;

    @Nullable
    private FlashMapManager flashMapManager;

    @Nullable
    private List<ViewResolver> viewResolvers;

    public DispatcherServlet() {
        super();
        setDispatchOptionsRequest(true);
    }

    public DispatcherServlet(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
        setDispatchOptionsRequest(true);
    }

    public void setDetectAllHandlerMappings(boolean detectAllHandlerMappings) {
        this.detectAllHandlerMappings = detectAllHandlerMappings;
    }

    public void setDetectAllHandlerAdapters(boolean detectAllHandlerAdapters) {
        this.detectAllHandlerAdapters = detectAllHandlerAdapters;
    }

    public void setDetectAllHandlerExceptionResolvers(boolean detectAllHandlerExceptionResolvers) {
        this.detectAllHandlerExceptionResolvers = detectAllHandlerExceptionResolvers;
    }

    public void setDetectAllViewResolvers(boolean detectAllViewResolvers) {
        this.detectAllViewResolvers = detectAllViewResolvers;
    }

    public void setThrowExceptionIfNoHandlerFound(boolean throwExceptionIfNoHandlerFound) {
        this.throwExceptionIfNoHandlerFound = throwExceptionIfNoHandlerFound;
    }

    public void setCleanupAfterInclude(boolean cleanupAfterInclude) {
        this.cleanupAfterInclude = cleanupAfterInclude;
    }

    @Override
    protected void onRefresh(ApplicationContext context) {
        initStrategies(context);
    }

    protected void initStrategies(ApplicationContext context) {
        initMultipartResolver(context); // 从BeanFactory实例化MultipartResolver 文件上传
        initLocaleResolver(context);
        initThemeResolver(context);
        /*
         * 从当前ApplicationContext及祖先ApplicationContext查找并实例化HandlerMapping类型的Bean
         * @WebMvcAutoConfiguration导入：RequestMappingHandlerMapping，BeanNameUrlHandlerMapping...
         * 加入到handlerMappings列表
         */
        initHandlerMappings(context); // 从BeanFactory实例化HandlerMapping 请求映射到处理器
        initHandlerAdapters(context); // 从BeanFactory实例化HandlerAdapter 适配请求与映射到的方法
        initHandlerExceptionResolvers(context); // 从BeanFactory实例化HandlerExceptionResolver 异常处理
        initRequestToViewNameTranslator(context); // 从BeanFactory实例化RequestToViewNameTranslator 解析请求到视图名
        initViewResolvers(context); // 从BeanFactory实例化ViewResolver 逻辑视图到具体视图
        initFlashMapManager(context);
    }

    private void initMultipartResolver(ApplicationContext context) {
        try {
            this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Detected " + this.multipartResolver);
            } else if (logger.isDebugEnabled()) {
                logger.debug("Detected " + this.multipartResolver.getClass().getSimpleName());
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // Default is no multipart resolver.
            this.multipartResolver = null;
            if (logger.isTraceEnabled()) {
                logger.trace("No MultipartResolver '" + MULTIPART_RESOLVER_BEAN_NAME + "' declared");
            }
        }
    }

    private void initLocaleResolver(ApplicationContext context) {
        try {
            this.localeResolver = context.getBean(LOCALE_RESOLVER_BEAN_NAME, LocaleResolver.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Detected " + this.localeResolver);
            } else if (logger.isDebugEnabled()) {
                logger.debug("Detected " + this.localeResolver.getClass().getSimpleName());
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.localeResolver = getDefaultStrategy(context, LocaleResolver.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No LocaleResolver '" + LOCALE_RESOLVER_BEAN_NAME + "': using default [" + this.localeResolver.getClass().getSimpleName() + "]");
            }
        }
    }

    private void initThemeResolver(ApplicationContext context) {
        try {
            this.themeResolver = context.getBean(THEME_RESOLVER_BEAN_NAME, ThemeResolver.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Detected " + this.themeResolver);
            } else if (logger.isDebugEnabled()) {
                logger.debug("Detected " + this.themeResolver.getClass().getSimpleName());
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.themeResolver = getDefaultStrategy(context, ThemeResolver.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No ThemeResolver '" + THEME_RESOLVER_BEAN_NAME + "': using default [" + this.themeResolver.getClass().getSimpleName() + "]");
            }
        }
    }

    private void initHandlerMappings(ApplicationContext context) {
        this.handlerMappings = null;
        // 默认
        if (this.detectAllHandlerMappings) {

            /*
             * 从当前ApplicationContext及祖先ApplicationContext查找并实例化HandlerMapping类型的Bean
             * @WebMvcAutoConfiguration导入：RequestMappingHandlerMapping，BeanNameUrlHandlerMapping
             */
            Map<String, HandlerMapping> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.handlerMappings = new ArrayList<>(matchingBeans.values());
                // We keep HandlerMappings in sorted order.
                AnnotationAwareOrderComparator.sort(this.handlerMappings);
            }
        } else {
            try {
                HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);
                this.handlerMappings = Collections.singletonList(hm);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, we'll add a default HandlerMapping later.
            }
        }
        // 确保至少有一个HandlerMapping
        if (this.handlerMappings == null) {
            // 默认的
            this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No HandlerMappings declared for servlet '" + getServletName() + "': using default strategies from DispatcherServlet.properties");
            }
        }
    }

    private void initHandlerAdapters(ApplicationContext context) {
        this.handlerAdapters = null;
        if (this.detectAllHandlerAdapters) {
            // Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.
            Map<String, HandlerAdapter> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.handlerAdapters = new ArrayList<>(matchingBeans.values());
                // We keep HandlerAdapters in sorted order.
                AnnotationAwareOrderComparator.sort(this.handlerAdapters);
            }
        } else {
            try {
                HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);
                this.handlerAdapters = Collections.singletonList(ha);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, we'll add a default HandlerAdapter later.
            }
        }
        // Ensure we have at least some HandlerAdapters, by registering
        // default HandlerAdapters if no other adapters are found.
        if (this.handlerAdapters == null) {
            this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No HandlerAdapters declared for servlet '" + getServletName() + "': using default strategies from DispatcherServlet.properties");
            }
        }
    }

    private void initHandlerExceptionResolvers(ApplicationContext context) {
        this.handlerExceptionResolvers = null;
        if (this.detectAllHandlerExceptionResolvers) {
            // Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.
            Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());
                // We keep HandlerExceptionResolvers in sorted order.
                AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);
            }
        } else {
            try {
                HandlerExceptionResolver her = context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);
                this.handlerExceptionResolvers = Collections.singletonList(her);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, no HandlerExceptionResolver is fine too.
            }
        }
        // Ensure we have at least some HandlerExceptionResolvers, by registering
        // default HandlerExceptionResolvers if no other resolvers are found.
        if (this.handlerExceptionResolvers == null) {
            this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No HandlerExceptionResolvers declared in servlet '" + getServletName() + "': using default strategies from DispatcherServlet.properties");
            }
        }
    }

    private void initRequestToViewNameTranslator(ApplicationContext context) {
        try {
            this.viewNameTranslator = context.getBean(REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME, RequestToViewNameTranslator.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Detected " + this.viewNameTranslator.getClass().getSimpleName());
            } else if (logger.isDebugEnabled()) {
                logger.debug("Detected " + this.viewNameTranslator);
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.viewNameTranslator = getDefaultStrategy(context, RequestToViewNameTranslator.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No RequestToViewNameTranslator '" + REQUEST_TO_VIEW_NAME_TRANSLATOR_BEAN_NAME + "': using default [" + this.viewNameTranslator.getClass().getSimpleName() + "]");
            }
        }
    }

    private void initViewResolvers(ApplicationContext context) {
        this.viewResolvers = null;
        if (this.detectAllViewResolvers) {
            // Find all ViewResolvers in the ApplicationContext, including ancestor contexts.
            Map<String, ViewResolver> matchingBeans = BeanFactoryUtils.beansOfTypeIncludingAncestors(context, ViewResolver.class, true, false);
            if (!matchingBeans.isEmpty()) {
                this.viewResolvers = new ArrayList<>(matchingBeans.values());
                // We keep ViewResolvers in sorted order.
                AnnotationAwareOrderComparator.sort(this.viewResolvers);
            }
        } else {
            try {
                ViewResolver vr = context.getBean(VIEW_RESOLVER_BEAN_NAME, ViewResolver.class);
                this.viewResolvers = Collections.singletonList(vr);
            } catch (NoSuchBeanDefinitionException ex) {
                // Ignore, we'll add a default ViewResolver later.
            }
        }
        // Ensure we have at least one ViewResolver, by registering
        // a default ViewResolver if no other resolvers are found.
        if (this.viewResolvers == null) {
            this.viewResolvers = getDefaultStrategies(context, ViewResolver.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No ViewResolvers declared for servlet '" + getServletName() + "': using default strategies from DispatcherServlet.properties");
            }
        }
    }

    private void initFlashMapManager(ApplicationContext context) {
        try {
            this.flashMapManager = context.getBean(FLASH_MAP_MANAGER_BEAN_NAME, FlashMapManager.class);
            if (logger.isTraceEnabled()) {
                logger.trace("Detected " + this.flashMapManager.getClass().getSimpleName());
            } else if (logger.isDebugEnabled()) {
                logger.debug("Detected " + this.flashMapManager);
            }
        } catch (NoSuchBeanDefinitionException ex) {
            // We need to use the default.
            this.flashMapManager = getDefaultStrategy(context, FlashMapManager.class);
            if (logger.isTraceEnabled()) {
                logger.trace("No FlashMapManager '" + FLASH_MAP_MANAGER_BEAN_NAME + "': using default [" + this.flashMapManager.getClass().getSimpleName() + "]");
            }
        }
    }

    @Nullable
    public final ThemeSource getThemeSource() {
        return (getWebApplicationContext() instanceof ThemeSource ? (ThemeSource) getWebApplicationContext() : null);
    }

    @Nullable
    public final MultipartResolver getMultipartResolver() {
        return this.multipartResolver;
    }

    @Nullable
    public final List<HandlerMapping> getHandlerMappings() {
        return (this.handlerMappings != null ? Collections.unmodifiableList(this.handlerMappings) : null);
    }

    protected <T> T getDefaultStrategy(ApplicationContext context, Class<T> strategyInterface) {
        List<T> strategies = getDefaultStrategies(context, strategyInterface);
        if (strategies.size() != 1) {
            throw new BeanInitializationException("DispatcherServlet needs exactly 1 strategy for interface [" + strategyInterface.getName() + "]");
        }
        return strategies.get(0);
    }

    /**
     * 如果在容器找不到对应的Bean，使用默认值，DispatcherServlet.properties
     */
    @SuppressWarnings("unchecked")
    protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {
        String key = strategyInterface.getName();
        String value = defaultStrategies.getProperty(key);
        if (value != null) {
            String[] classNames = StringUtils.commaDelimitedListToStringArray(value);
            List<T> strategies = new ArrayList<>(classNames.length);
            for (String className : classNames) {
                try {
                    Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());
                    Object strategy = createDefaultStrategy(context, clazz);
                    strategies.add((T) strategy);
                } catch (ClassNotFoundException ex) {
                    throw new BeanInitializationException("Could not find DispatcherServlet's default strategy class [" + className + "] for interface [" + key + "]", ex);
                } catch (LinkageError err) {
                    throw new BeanInitializationException("Unresolvable class definition for DispatcherServlet's default strategy class [" + className + "] for interface [" + key + "]", err);
                }
            }
            return strategies;
        } else {
            return new LinkedList<>();
        }
    }

    protected Object createDefaultStrategy(ApplicationContext context, Class<?> clazz) {
        return context.getAutowireCapableBeanFactory().createBean(clazz);
    }

    /**
     *
     */
    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logRequest(request);
        // Keep a snapshot of the request attributes in case of an include,
        // to be able to restore the original attributes after the include.
        Map<String, Object> attributesSnapshot = null;
        if (WebUtils.isIncludeRequest(request)) {
            attributesSnapshot = new HashMap<>();
            Enumeration<?> attrNames = request.getAttributeNames();
            while (attrNames.hasMoreElements()) {
                String attrName = (String) attrNames.nextElement();
                if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
                    attributesSnapshot.put(attrName, request.getAttribute(attrName));
                }
            }
        }
        // 把Spring的东西加到HttpServletRequest的属性上
        request.setAttribute(WEB_APPLICATION_CONTEXT_ATTRIBUTE, getWebApplicationContext());
        request.setAttribute(LOCALE_RESOLVER_ATTRIBUTE, this.localeResolver);
        request.setAttribute(THEME_RESOLVER_ATTRIBUTE, this.themeResolver);
        request.setAttribute(THEME_SOURCE_ATTRIBUTE, getThemeSource());
        if (this.flashMapManager != null) {
            FlashMap inputFlashMap = this.flashMapManager.retrieveAndUpdate(request, response);
            if (inputFlashMap != null) {
                request.setAttribute(INPUT_FLASH_MAP_ATTRIBUTE, Collections.unmodifiableMap(inputFlashMap));
            }
            request.setAttribute(OUTPUT_FLASH_MAP_ATTRIBUTE, new FlashMap());
            request.setAttribute(FLASH_MAP_MANAGER_ATTRIBUTE, this.flashMapManager);
        }
        try {
            // 分发处理
            doDispatch(request, response);
        } finally {
            if (!WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
                // Restore the original attribute snapshot, in case of an include.
                if (attributesSnapshot != null) {
                    restoreAttributesAfterInclude(request, attributesSnapshot);
                }
            }
        }
    }

    private void logRequest(HttpServletRequest request) {
        LogFormatUtils.traceDebug(logger, traceOn -> {
            String params;
            if (isEnableLoggingRequestDetails()) {
                params = request.getParameterMap().entrySet().stream().map(entry -> entry.getKey() + ":" + Arrays.toString(entry.getValue())).collect(Collectors.joining(", "));
            } else {
                params = (request.getParameterMap().isEmpty() ? "" : "masked");
            }
            String queryString = request.getQueryString();
            String queryClause = (StringUtils.hasLength(queryString) ? "?" + queryString : "");
            String dispatchType = (!request.getDispatcherType().equals(DispatcherType.REQUEST) ? "\"" + request.getDispatcherType().name() + "\" dispatch for " : "");
            String message = (dispatchType + request.getMethod() + " \"" + getRequestUri(request) + queryClause + "\", parameters={" + params + "}");
            if (traceOn) {
                List<String> values = Collections.list(request.getHeaderNames());
                String headers = values.size() > 0 ? "masked" : "";
                if (isEnableLoggingRequestDetails()) {
                    headers = values.stream().map(name -> name + ":" + Collections.list(request.getHeaders(name))).collect(Collectors.joining(", "));
                }
                return message + ", headers={" + headers + "} in DispatcherServlet '" + getServletName() + "'";
            } else {
                return message;
            }
        });
    }

    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        // 执行链
        HandlerExecutionChain mappedHandler = null;
        boolean multipartRequestParsed = false;
        WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);
        try {
            ModelAndView mv = null;
            Exception dispatchException = null;
            try {
                // 如果是文件上传，将request封装为MultipartHttpServletRequest
                processedRequest = checkMultipart(request);
                // processedRequest和request不是同一个对象，说明是文件上传请求，解析了文件，后面需要清理
                multipartRequestParsed = (processedRequest != request);
                /*
                 * 遍历，HandlerMapping#getHandler，返回第一个不为空的HandlerExecutionChain，假设是RequestMappingHandlerMapping返回
                 * 根据url获取HandlerMethod，并获得适用的HandlerInterceptor，生成HandlerExecutionChain
                 */
                mappedHandler = getHandler(processedRequest);
                if (mappedHandler == null) {
                    // 404
                    noHandlerFound(processedRequest, response);
                    return;
                }
                // 遍历，返回第一个支持的HandlerAdapter，假设是RequestMappingHandlerAdapter
                HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
                // Process last-modified header, if supported by the handler.
                String method = request.getMethod();
                boolean isGet = "GET".equals(method);
                if (isGet || "HEAD".equals(method)) {
                    long lastModified = ha.getLastModified(request, mappedHandler.getHandler());
                    // 上次请求后没有修改，直接返回响应
                    if (new ServletWebRequest(request, response).checkNotModified(lastModified) && isGet) {
                        return;
                    }
                }
                // 执行拦截器，HandlerInterceptor#preHandle
                if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                    return;
                }
                /*
                 * 执行方法，返回ModelAndView，太长不想细看
                 * HandlerAdapter#handler
                 * AbstractHandlerMethodAdapter#handle
                 * RequestMappingHandlerAdapter#handleInternal
                 * RequestMappingHandlerAdapter#invokeHandlerMethod
                 * ServletInvocableHandlerMethod#invokeAndHandle
                 * InvocableHandlerMethod#invokeForRequest
                 * InvocableHandlerMethod#doInvoke
                 */
                mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
                if (asyncManager.isConcurrentHandlingStarted()) {
                    return;
                }
                // 如果没有View，设置ModelAndView的ViewName
                applyDefaultViewName(processedRequest, mv);
                // 执行拦截器，逆序，HandlerInterceptor#postHandle
                mappedHandler.applyPostHandle(processedRequest, response, mv);
            } catch (Exception ex) {
                dispatchException = ex;
            } catch (Throwable err) {
                // As of 4.3, we're processing Errors thrown from handler methods as well,
                // making them available for @ExceptionHandler methods and other scenarios.
                dispatchException = new NestedServletException("Handler dispatch failed", err);
            }
            // ViewResolver根据ModelAndView得到View，跳转到指定页面，调用拦截器的AfterCompletion
            processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
        } catch (Exception ex) {
            // 异常也要调用，倒序执行，HandlerInterceptor#afterCompletion
            triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
        } catch (Throwable err) {
            // 异常也要调用拦截器的AfterCompletion
            triggerAfterCompletion(processedRequest, response, mappedHandler, new NestedServletException("Handler processing failed", err));
        } finally {
            if (asyncManager.isConcurrentHandlingStarted()) {
                // Instead of postHandle and afterCompletion
                if (mappedHandler != null) {
                    mappedHandler.applyAfterConcurrentHandlingStarted(processedRequest, response);
                }
            } else {
                // Clean up any resources used by a multipart request.
                if (multipartRequestParsed) {
                    cleanupMultipart(processedRequest);
                }
            }
        }
    }

    private void applyDefaultViewName(HttpServletRequest request, @Nullable ModelAndView mv) throws Exception {
        if (mv != null && !mv.hasView()) {
            String defaultViewName = getDefaultViewName(request);
            if (defaultViewName != null) {
                mv.setViewName(defaultViewName);
            }
        }
    }

    /**
     *
     */
    private void processDispatchResult(HttpServletRequest request, HttpServletResponse response, @Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv, @Nullable Exception exception) throws Exception {
        boolean errorView = false;
        // HandlerMapping、HandlerAdapter处理时的异常是否为空
        if (exception != null) {
            // ModelAndViewDefiningException
            if (exception instanceof ModelAndViewDefiningException) {
                logger.debug("ModelAndViewDefiningException encountered", exception);
                // 获取异常视图
                mv = ((ModelAndViewDefiningException) exception).getModelAndView();
            } else {
                Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
                mv = processHandlerException(request, response, handler, exception);
                errorView = (mv != null);
            }
        }
        // Did the handler return a view to render?
        if (mv != null && !mv.wasCleared()) {
            // 渲染
            render(mv, request, response);
            if (errorView) {
                WebUtils.clearErrorRequestAttributes(request);
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("No view rendering, null ModelAndView returned.");
            }
        }
        if (WebAsyncUtils.getAsyncManager(request).isConcurrentHandlingStarted()) {
            // Concurrent handling started during a forward
            return;
        }
        if (mappedHandler != null) {
            // HandlerInterceptor#afterCompletion
            mappedHandler.triggerAfterCompletion(request, response, null);
        }
    }

    @Override
    protected LocaleContext buildLocaleContext(final HttpServletRequest request) {
        LocaleResolver lr = this.localeResolver;
        if (lr instanceof LocaleContextResolver) {
            return ((LocaleContextResolver) lr).resolveLocaleContext(request);
        } else {
            return () -> (lr != null ? lr.resolveLocale(request) : request.getLocale());
        }
    }

    protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {
        if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
            if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {
                if (request.getDispatcherType().equals(DispatcherType.REQUEST)) {
                    logger.trace("Request already resolved to MultipartHttpServletRequest, e.g. by MultipartFilter");
                }
            } else if (hasMultipartException(request)) {
                logger.debug("Multipart resolution previously failed for current request - " + "skipping re-resolution for undisturbed error rendering");
            } else {
                try {
                    return this.multipartResolver.resolveMultipart(request);
                } catch (MultipartException ex) {
                    if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {
                        logger.debug("Multipart resolution failed for error dispatch", ex);
                        // Keep processing error dispatch with regular request handle below
                    } else {
                        throw ex;
                    }
                }
            }
        }
        // If not returned before: return original request.
        return request;
    }

    private boolean hasMultipartException(HttpServletRequest request) {
        Throwable error = (Throwable) request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE);
        while (error != null) {
            if (error instanceof MultipartException) {
                return true;
            }
            error = error.getCause();
        }
        return false;
    }

    protected void cleanupMultipart(HttpServletRequest request) {
        if (this.multipartResolver != null) {
            MultipartHttpServletRequest multipartRequest = WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class);
            if (multipartRequest != null) {
                this.multipartResolver.cleanupMultipart(multipartRequest);
            }
        }
    }

    @Nullable
    protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
        if (this.handlerMappings != null) {
            for (HandlerMapping mapping : this.handlerMappings) {
                HandlerExecutionChain handler = mapping.getHandler(request);
                if (handler != null) {
                    return handler;
                }
            }
        }
        return null;
    }

    protected void noHandlerFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (pageNotFoundLogger.isWarnEnabled()) {
            pageNotFoundLogger.warn("No mapping for " + request.getMethod() + " " + getRequestUri(request));
        }
        if (this.throwExceptionIfNoHandlerFound) {
            throw new NoHandlerFoundException(request.getMethod(), getRequestUri(request), new ServletServerHttpRequest(request).getHeaders());
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {
        if (this.handlerAdapters != null) {
            for (HandlerAdapter adapter : this.handlerAdapters) {
                if (adapter.supports(handler)) {
                    return adapter;
                }
            }
        }
        throw new ServletException("No adapter for handler [" + handler + "]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");
    }

    @Nullable
    protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) throws Exception {
        // Success and error responses may use different content types
        request.removeAttribute(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
        // Check registered HandlerExceptionResolvers...
        ModelAndView exMv = null;
        if (this.handlerExceptionResolvers != null) {
            for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
                exMv = resolver.resolveException(request, response, handler, ex);
                if (exMv != null) {
                    break;
                }
            }
        }
        if (exMv != null) {
            if (exMv.isEmpty()) {
                request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
                return null;
            }
            // We might still need view name translation for a plain error model...
            if (!exMv.hasView()) {
                String defaultViewName = getDefaultViewName(request);
                if (defaultViewName != null) {
                    exMv.setViewName(defaultViewName);
                }
            }
            if (logger.isTraceEnabled()) {
                logger.trace("Using resolved error view: " + exMv, ex);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Using resolved error view: " + exMv);
            }
            WebUtils.exposeErrorRequestAttributes(request, ex, getServletName());
            return exMv;
        }
        throw ex;
    }

    protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Determine locale for request and apply it to the response.
        Locale locale = (this.localeResolver != null ? this.localeResolver.resolveLocale(request) : request.getLocale());
        response.setLocale(locale);
        View view;
        String viewName = mv.getViewName();
        if (viewName != null) {
            // 获取对应的View
            view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
            if (view == null) {
                throw new ServletException("Could not resolve view with name '" + mv.getViewName() + "' in servlet with name '" + getServletName() + "'");
            }
        } else {
            // No need to lookup: the ModelAndView object contains the actual View object.
            view = mv.getView();
            if (view == null) {
                throw new ServletException("ModelAndView [" + mv + "] neither contains a view name nor a " + "View object in servlet with name '" + getServletName() + "'");
            }
        }
        // Delegate to the View object for rendering.
        if (logger.isTraceEnabled()) {
            logger.trace("Rendering view [" + view + "] ");
        }
        try {
            if (mv.getStatus() != null) {
                // 设置Http响应状态
                response.setStatus(mv.getStatus().value());
            }
            // 渲染视图，AbstractView#render
            view.render(mv.getModelInternal(), request, response);
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error rendering view [" + view + "]", ex);
            }
            throw ex;
        }
    }

    @Nullable
    protected String getDefaultViewName(HttpServletRequest request) throws Exception {
        return (this.viewNameTranslator != null ? this.viewNameTranslator.getViewName(request) : null);
    }

    @Nullable
    protected View resolveViewName(String viewName, @Nullable Map<String, Object> model, Locale locale, HttpServletRequest request) throws Exception {
        if (this.viewResolvers != null) {
            for (ViewResolver viewResolver : this.viewResolvers) {
                View view = viewResolver.resolveViewName(viewName, locale);
                if (view != null) {
                    return view;
                }
            }
        }
        return null;
    }

    private void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, @Nullable HandlerExecutionChain mappedHandler, Exception ex) throws Exception {
        if (mappedHandler != null) {
            mappedHandler.triggerAfterCompletion(request, response, ex);
        }
        throw ex;
    }

    @SuppressWarnings("unchecked")
    private void restoreAttributesAfterInclude(HttpServletRequest request, Map<?, ?> attributesSnapshot) {
        // Need to copy into separate Collection here, to avoid side effects
        // on the Enumeration when removing attributes.
        Set<String> attrsToCheck = new HashSet<>();
        Enumeration<?> attrNames = request.getAttributeNames();
        while (attrNames.hasMoreElements()) {
            String attrName = (String) attrNames.nextElement();
            if (this.cleanupAfterInclude || attrName.startsWith(DEFAULT_STRATEGIES_PREFIX)) {
                attrsToCheck.add(attrName);
            }
        }
        // Add attributes that may have been removed
        attrsToCheck.addAll((Set<String>) attributesSnapshot.keySet());
        // Iterate over the attributes to check, restoring the original value
        // or removing the attribute, respectively, if appropriate.
        for (String attrName : attrsToCheck) {
            Object attrValue = attributesSnapshot.get(attrName);
            if (attrValue == null) {
                request.removeAttribute(attrName);
            } else if (attrValue != request.getAttribute(attrName)) {
                request.setAttribute(attrName, attrValue);
            }
        }
    }

    private static String getRequestUri(HttpServletRequest request) {
        String uri = (String) request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }
        return uri;
    }

}
