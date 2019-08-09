package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.*;
import org.apache.catalina.WebResourceRoot.ResourceSetType;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.session.StandardManager;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.catalina.util.LifecycleBase;
import org.apache.catalina.webresources.AbstractResourceSet;
import org.apache.catalina.webresources.EmptyResource;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContainerInitializer;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class TomcatServletWebServerFactory extends AbstractServletWebServerFactory implements ConfigurableTomcatWebServerFactory, ResourceLoaderAware {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final Set<Class<?>> NO_CLASSES = Collections.emptySet();

    public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

    private File baseDirectory;

    private List<Valve> engineValves = new ArrayList<>();

    private List<Valve> contextValves = new ArrayList<>();

    private List<LifecycleListener> contextLifecycleListeners = getDefaultLifecycleListeners();

    private List<TomcatContextCustomizer> tomcatContextCustomizers = new ArrayList<>();

    private List<TomcatConnectorCustomizer> tomcatConnectorCustomizers = new ArrayList<>();

    private List<TomcatProtocolHandlerCustomizer<?>> tomcatProtocolHandlerCustomizers = new ArrayList<>();

    private final List<Connector> additionalTomcatConnectors = new ArrayList<>();

    private ResourceLoader resourceLoader;

    private String protocol = DEFAULT_PROTOCOL;

    private Set<String> tldSkipPatterns = new LinkedHashSet<>(TldSkipPatterns.DEFAULT);

    private Charset uriEncoding = DEFAULT_CHARSET;

    private int backgroundProcessorDelay;

    public TomcatServletWebServerFactory() {
    }

    public TomcatServletWebServerFactory(int port) {
        super(port);
    }

    public TomcatServletWebServerFactory(String contextPath, int port) {
        super(contextPath, port);
    }

    private static List<LifecycleListener> getDefaultLifecycleListeners() {
        AprLifecycleListener aprLifecycleListener = new AprLifecycleListener();
        return AprLifecycleListener.isAprAvailable() ? new ArrayList<>(Arrays.asList(aprLifecycleListener)) : new ArrayList<>();
    }

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        // 实例化Tomcat
        Tomcat tomcat = new Tomcat();
        // 临时目录
        File baseDir = (this.baseDirectory != null) ? this.baseDirectory : createTempDir("tomcat");
        tomcat.setBaseDir(baseDir.getAbsolutePath());
        // 创建Connector，默认协议NIO
        Connector connector = new Connector(this.protocol);
        tomcat.getService().addConnector(connector);
        // 定制Connector：端口，UriEncoding，SSL，压缩
        customizeConnector(connector);
        tomcat.setConnector(connector);
        // 关闭应用的自动部署
        tomcat.getHost().setAutoDeploy(false);
        configureEngine(tomcat.getEngine());
        // 如果有附加的Connector，也添加进来
        // Tomcat Service = 1 Tomcat Engine + N Tomcat Connector
        for (Connector additionalConnector : this.additionalTomcatConnectors) {
            // 创建StandardServer
            tomcat.getService().addConnector(additionalConnector);
        }
        //
        prepareContext(tomcat.getHost(), initializers);
        return getTomcatWebServer(tomcat);
    }

    private void configureEngine(Engine engine) {
        engine.setBackgroundProcessorDelay(this.backgroundProcessorDelay);
        for (Valve valve : this.engineValves) {
            engine.getPipeline().addValve(valve);
        }
    }

    protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
        File documentRoot = getValidDocumentRoot();
        // 创建StandardContext，表示一个Web应用
        TomcatEmbeddedContext context = new TomcatEmbeddedContext();
        if (documentRoot != null) {
            context.setResources(new LoaderHidingResourceRoot(context));
        }
        context.setName(getContextPath());
        context.setDisplayName(getDisplayName());
        context.setPath(getContextPath());
        File docBase = (documentRoot != null) ? documentRoot : createTempDir("tomcat-docbase");
        context.setDocBase(docBase.getAbsolutePath());
        context.addLifecycleListener(new FixContextListener());
        context.setParentClassLoader((this.resourceLoader != null) ? this.resourceLoader.getClassLoader() : ClassUtils.getDefaultClassLoader());
        resetDefaultLocaleMapping(context);
        addLocaleMappings(context);
        context.setUseRelativeRedirects(false);
        try {
            context.setCreateUploadTargets(true);
        } catch (NoSuchMethodError ex) {
            // Tomcat is < 8.5.39. Continue.
        }
        configureTldSkipPatterns(context);
        WebappLoader loader = new WebappLoader(context.getParentClassLoader());
        loader.setLoaderClass(TomcatEmbeddedWebappClassLoader.class.getName());
        loader.setDelegate(true);
        context.setLoader(loader);
        if (isRegisterDefaultServlet()) {
            // 默认会注册Tomcat的DefaultServlet
            addDefaultServlet(context);
        }
        // 默认内嵌Tomcat，不注册JspServlet
        if (shouldRegisterJspServlet()) {
            addJspServlet(context);
            addJasperInitializer(context);
        }
        context.addLifecycleListener(new StaticResourceConfigurer(context));
        ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
        host.addChild(context);
        //
        configureContext(context, initializersToUse);
        postProcessContext(context);
    }

    private void resetDefaultLocaleMapping(TomcatEmbeddedContext context) {
        context.addLocaleEncodingMappingParameter(Locale.ENGLISH.toString(), DEFAULT_CHARSET.displayName());
        context.addLocaleEncodingMappingParameter(Locale.FRENCH.toString(), DEFAULT_CHARSET.displayName());
    }

    private void addLocaleMappings(TomcatEmbeddedContext context) {
        getLocaleCharsetMappings().forEach((locale, charset) -> context.addLocaleEncodingMappingParameter(locale.toString(), charset.toString()));
    }

    private void configureTldSkipPatterns(TomcatEmbeddedContext context) {
        StandardJarScanFilter filter = new StandardJarScanFilter();
        filter.setTldSkip(StringUtils.collectionToCommaDelimitedString(this.tldSkipPatterns));
        context.getJarScanner().setJarScanFilter(filter);
    }

    private void addDefaultServlet(Context context) {
        Wrapper defaultServlet = context.createWrapper();
        defaultServlet.setName("default");
        defaultServlet.setServletClass("org.apache.catalina.servlets.DefaultServlet");
        defaultServlet.addInitParameter("debug", "0");
        defaultServlet.addInitParameter("listings", "false");
        defaultServlet.setLoadOnStartup(1);
        // Otherwise the default location of a Spring DispatcherServlet cannot be set
        defaultServlet.setOverridable(true);
        context.addChild(defaultServlet);
        context.addServletMappingDecoded("/", "default");
    }

    private void addJspServlet(Context context) {
        Wrapper jspServlet = context.createWrapper();
        jspServlet.setName("jsp");
        jspServlet.setServletClass(getJsp().getClassName());
        jspServlet.addInitParameter("fork", "false");
        getJsp().getInitParameters().forEach(jspServlet::addInitParameter);
        jspServlet.setLoadOnStartup(3);
        context.addChild(jspServlet);
        context.addServletMappingDecoded("*.jsp", "jsp");
        context.addServletMappingDecoded("*.jspx", "jsp");
    }

    private void addJasperInitializer(TomcatEmbeddedContext context) {
        try {
            ServletContainerInitializer initializer = (ServletContainerInitializer) ClassUtils.forName("org.apache.jasper.servlet.JasperInitializer", null).newInstance();
            context.addServletContainerInitializer(initializer, null);
        } catch (Exception ex) {
            // Probably not Tomcat 8
        }
    }

    // Needs to be protected so it can be used by subclasses
    protected void customizeConnector(Connector connector) {
        int port = (getPort() >= 0) ? getPort() : 0;
        connector.setPort(port);
        if (StringUtils.hasText(this.getServerHeader())) {
            connector.setAttribute("server", this.getServerHeader());
        }
        if (connector.getProtocolHandler() instanceof AbstractProtocol) {
            customizeProtocol((AbstractProtocol<?>) connector.getProtocolHandler());
        }
        invokeProtocolHandlerCustomizers(connector.getProtocolHandler());
        if (getUriEncoding() != null) {
            connector.setURIEncoding(getUriEncoding().name());
        }
        // Don't bind to the socket prematurely if ApplicationContext is slow to start
        connector.setProperty("bindOnInit", "false");
        if (getSsl() != null && getSsl().isEnabled()) {
            customizeSsl(connector);
        }
        TomcatConnectorCustomizer compression = new CompressionConnectorCustomizer(getCompression());
        compression.customize(connector);
        for (TomcatConnectorCustomizer customizer : this.tomcatConnectorCustomizers) {
            customizer.customize(connector);
        }
    }

    private void customizeProtocol(AbstractProtocol<?> protocol) {
        if (getAddress() != null) {
            protocol.setAddress(getAddress());
        }
    }

    @SuppressWarnings("unchecked")
    private void invokeProtocolHandlerCustomizers(ProtocolHandler protocolHandler) {
        LambdaSafe.callbacks(TomcatProtocolHandlerCustomizer.class, this.tomcatProtocolHandlerCustomizers, protocolHandler).invoke((customizer) -> customizer.customize(protocolHandler));
    }

    private void customizeSsl(Connector connector) {
        new SslConnectorCustomizer(getSsl(), getSslStoreProvider()).customize(connector);
        if (getHttp2() != null && getHttp2().isEnabled()) {
            connector.addUpgradeProtocol(new Http2Protocol());
        }
    }

    protected void configureContext(Context context, ServletContextInitializer[] initializers) {
        // 继承了ServletContainerInitializer
        TomcatStarter starter = new TomcatStarter(initializers);
        if (context instanceof TomcatEmbeddedContext) {
            TomcatEmbeddedContext embeddedContext = (TomcatEmbeddedContext) context;
            // 添加
            embeddedContext.setStarter(starter);
            embeddedContext.setFailCtxIfServletStartFails(true);
        }
        context.addServletContainerInitializer(starter, NO_CLASSES);
        for (LifecycleListener lifecycleListener : this.contextLifecycleListeners) {
            context.addLifecycleListener(lifecycleListener);
        }
        for (Valve valve : this.contextValves) {
            context.getPipeline().addValve(valve);
        }
        for (ErrorPage errorPage : getErrorPages()) {
            new TomcatErrorPage(errorPage).addToContext(context);
        }
        for (MimeMappings.Mapping mapping : getMimeMappings()) {
            context.addMimeMapping(mapping.getExtension(), mapping.getMimeType());
        }
        configureSession(context);
        new DisableReferenceClearingContextCustomizer().customize(context);
        for (TomcatContextCustomizer customizer : this.tomcatContextCustomizers) {
            customizer.customize(context);
        }
    }

    private void configureSession(Context context) {
        long sessionTimeout = getSessionTimeoutInMinutes();
        context.setSessionTimeout((int) sessionTimeout);
        Boolean httpOnly = getSession().getCookie().getHttpOnly();
        if (httpOnly != null) {
            context.setUseHttpOnly(httpOnly);
        }
        if (getSession().isPersistent()) {
            Manager manager = context.getManager();
            if (manager == null) {
                manager = new StandardManager();
                context.setManager(manager);
            }
            configurePersistSession(manager);
        } else {
            context.addLifecycleListener(new DisablePersistSessionListener());
        }
    }

    private void configurePersistSession(Manager manager) {
        Assert.state(manager instanceof StandardManager, () -> "Unable to persist HTTP session state using manager type " + manager.getClass().getName());
        File dir = getValidSessionStoreDir();
        File file = new File(dir, "SESSIONS.ser");
        ((StandardManager) manager).setPathname(file.getAbsolutePath());
    }

    private long getSessionTimeoutInMinutes() {
        Duration sessionTimeout = getSession().getTimeout();
        if (isZeroOrLess(sessionTimeout)) {
            return 0;
        }
        return Math.max(sessionTimeout.toMinutes(), 1);
    }

    private boolean isZeroOrLess(Duration sessionTimeout) {
        return sessionTimeout == null || sessionTimeout.isNegative() || sessionTimeout.isZero();
    }

    protected void postProcessContext(Context context) {
    }

    protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
        return new TomcatWebServer(tomcat, getPort() >= 0);
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public Set<String> getTldSkipPatterns() {
        return this.tldSkipPatterns;
    }

    public void setTldSkipPatterns(Collection<String> patterns) {
        Assert.notNull(patterns, "Patterns must not be null");
        this.tldSkipPatterns = new LinkedHashSet<>(patterns);
    }

    public void addTldSkipPatterns(String... patterns) {
        Assert.notNull(patterns, "Patterns must not be null");
        this.tldSkipPatterns.addAll(Arrays.asList(patterns));
    }

    public void setProtocol(String protocol) {
        Assert.hasLength(protocol, "Protocol must not be empty");
        this.protocol = protocol;
    }

    public void setEngineValves(Collection<? extends Valve> engineValves) {
        Assert.notNull(engineValves, "Valves must not be null");
        this.engineValves = new ArrayList<>(engineValves);
    }

    public Collection<Valve> getEngineValves() {
        return this.engineValves;
    }

    @Override
    public void addEngineValves(Valve... engineValves) {
        Assert.notNull(engineValves, "Valves must not be null");
        this.engineValves.addAll(Arrays.asList(engineValves));
    }

    public void setContextValves(Collection<? extends Valve> contextValves) {
        Assert.notNull(contextValves, "Valves must not be null");
        this.contextValves = new ArrayList<>(contextValves);
    }

    public Collection<Valve> getContextValves() {
        return this.contextValves;
    }

    public void addContextValves(Valve... contextValves) {
        Assert.notNull(contextValves, "Valves must not be null");
        this.contextValves.addAll(Arrays.asList(contextValves));
    }

    public void setContextLifecycleListeners(Collection<? extends LifecycleListener> contextLifecycleListeners) {
        Assert.notNull(contextLifecycleListeners, "ContextLifecycleListeners must not be null");
        this.contextLifecycleListeners = new ArrayList<>(contextLifecycleListeners);
    }

    public Collection<LifecycleListener> getContextLifecycleListeners() {
        return this.contextLifecycleListeners;
    }

    public void addContextLifecycleListeners(LifecycleListener... contextLifecycleListeners) {
        Assert.notNull(contextLifecycleListeners, "ContextLifecycleListeners must not be null");
        this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
    }

    public void setTomcatContextCustomizers(Collection<? extends TomcatContextCustomizer> tomcatContextCustomizers) {
        Assert.notNull(tomcatContextCustomizers, "TomcatContextCustomizers must not be null");
        this.tomcatContextCustomizers = new ArrayList<>(tomcatContextCustomizers);
    }

    public Collection<TomcatContextCustomizer> getTomcatContextCustomizers() {
        return this.tomcatContextCustomizers;
    }

    @Override
    public void addContextCustomizers(TomcatContextCustomizer... tomcatContextCustomizers) {
        Assert.notNull(tomcatContextCustomizers, "TomcatContextCustomizers must not be null");
        this.tomcatContextCustomizers.addAll(Arrays.asList(tomcatContextCustomizers));
    }

    public void setTomcatConnectorCustomizers(Collection<? extends TomcatConnectorCustomizer> tomcatConnectorCustomizers) {
        Assert.notNull(tomcatConnectorCustomizers, "TomcatConnectorCustomizers must not be null");
        this.tomcatConnectorCustomizers = new ArrayList<>(tomcatConnectorCustomizers);
    }

    @Override
    public void addConnectorCustomizers(TomcatConnectorCustomizer... tomcatConnectorCustomizers) {
        Assert.notNull(tomcatConnectorCustomizers, "TomcatConnectorCustomizers must not be null");
        this.tomcatConnectorCustomizers.addAll(Arrays.asList(tomcatConnectorCustomizers));
    }

    public Collection<TomcatConnectorCustomizer> getTomcatConnectorCustomizers() {
        return this.tomcatConnectorCustomizers;
    }

    public void setTomcatProtocolHandlerCustomizers(Collection<? extends TomcatProtocolHandlerCustomizer<?>> tomcatProtocolHandlerCustomizer) {
        Assert.notNull(tomcatProtocolHandlerCustomizer, "TomcatProtocolHandlerCustomizers must not be null");
        this.tomcatProtocolHandlerCustomizers = new ArrayList<>(tomcatProtocolHandlerCustomizer);
    }

    @Override
    public void addProtocolHandlerCustomizers(TomcatProtocolHandlerCustomizer<?>... tomcatProtocolHandlerCustomizers) {
        Assert.notNull(tomcatProtocolHandlerCustomizers, "TomcatProtocolHandlerCustomizers must not be null");
        this.tomcatProtocolHandlerCustomizers.addAll(Arrays.asList(tomcatProtocolHandlerCustomizers));
    }

    public Collection<TomcatProtocolHandlerCustomizer<?>> getTomcatProtocolHandlerCustomizers() {
        return this.tomcatProtocolHandlerCustomizers;
    }

    public void addAdditionalTomcatConnectors(Connector... connectors) {
        Assert.notNull(connectors, "Connectors must not be null");
        this.additionalTomcatConnectors.addAll(Arrays.asList(connectors));
    }

    public List<Connector> getAdditionalTomcatConnectors() {
        return this.additionalTomcatConnectors;
    }

    @Override
    public void setUriEncoding(Charset uriEncoding) {
        this.uriEncoding = uriEncoding;
    }

    public Charset getUriEncoding() {
        return this.uriEncoding;
    }

    @Override
    public void setBackgroundProcessorDelay(int delay) {
        this.backgroundProcessorDelay = delay;
    }

    private static class DisablePersistSessionListener implements LifecycleListener {

        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (event.getType().equals(Lifecycle.START_EVENT)) {
                Context context = (Context) event.getLifecycle();
                Manager manager = context.getManager();
                if (manager instanceof StandardManager) {
                    ((StandardManager) manager).setPathname(null);
                }
            }
        }

    }

    private final class StaticResourceConfigurer implements LifecycleListener {

        private final Context context;

        private StaticResourceConfigurer(Context context) {
            this.context = context;
        }

        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                addResourceJars(getUrlsOfJarsWithMetaInfResources());
            }
        }

        private void addResourceJars(List<URL> resourceJarUrls) {
            for (URL url : resourceJarUrls) {
                String path = url.getPath();
                if (path.endsWith(".jar") || path.endsWith(".jar!/")) {
                    String jar = url.toString();
                    if (!jar.startsWith("jar:")) {
                        // A jar file in the file system. Convert to Jar URL.
                        jar = "jar:" + jar + "!/";
                    }
                    addResourceSet(jar);
                } else {
                    addResourceSet(url.toString());
                }
            }
        }

        private void addResourceSet(String resource) {
            try {
                if (isInsideNestedJar(resource)) {
                    // It's a nested jar but we now don't want the suffix because Tomcat
                    // is going to try and locate it as a root URL (not the resource
                    // inside it)
                    resource = resource.substring(0, resource.length() - 2);
                }
                URL url = new URL(resource);
                String path = "/META-INF/resources";
                this.context.getResources().createWebResourceSet(ResourceSetType.RESOURCE_JAR, "/", url, path);
            } catch (Exception ex) {
                // Ignore (probably not a directory)
            }
        }

        private boolean isInsideNestedJar(String dir) {
            return dir.indexOf("!/") < dir.lastIndexOf("!/");
        }

    }

    private static final class LoaderHidingResourceRoot extends StandardRoot {

        private LoaderHidingResourceRoot(TomcatEmbeddedContext context) {
            super(context);
        }

        @Override
        protected WebResourceSet createMainResourceSet() {
            return new LoaderHidingWebResourceSet(super.createMainResourceSet());
        }

    }

    private static final class LoaderHidingWebResourceSet extends AbstractResourceSet {

        private final WebResourceSet delegate;

        private final Method initInternal;

        private LoaderHidingWebResourceSet(WebResourceSet delegate) {
            this.delegate = delegate;
            try {
                this.initInternal = LifecycleBase.class.getDeclaredMethod("initInternal");
                this.initInternal.setAccessible(true);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public WebResource getResource(String path) {
            if (path.startsWith("/org/springframework/boot")) {
                return new EmptyResource(getRoot(), path);
            }
            return this.delegate.getResource(path);
        }

        @Override
        public String[] list(String path) {
            return this.delegate.list(path);
        }

        @Override
        public Set<String> listWebAppPaths(String path) {
            return this.delegate.listWebAppPaths(path);
        }

        @Override
        public boolean mkdir(String path) {
            return this.delegate.mkdir(path);
        }

        @Override
        public boolean write(String path, InputStream is, boolean overwrite) {
            return this.delegate.write(path, is, overwrite);
        }

        @Override
        public URL getBaseUrl() {
            return this.delegate.getBaseUrl();
        }

        @Override
        public void setReadOnly(boolean readOnly) {
            this.delegate.setReadOnly(readOnly);
        }

        @Override
        public boolean isReadOnly() {
            return this.delegate.isReadOnly();
        }

        @Override
        public void gc() {
            this.delegate.gc();
        }

        @Override
        protected void initInternal() throws LifecycleException {
            if (this.delegate instanceof LifecycleBase) {
                try {
                    ReflectionUtils.invokeMethod(this.initInternal, this.delegate);
                } catch (Exception ex) {
                    throw new LifecycleException(ex);
                }
            }
        }

    }

}
