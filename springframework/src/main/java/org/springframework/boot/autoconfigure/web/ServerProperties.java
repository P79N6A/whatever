package org.springframework.boot.autoconfigure.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.servlet.server.Jsp;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
// import io.undertow.UndertowOptions;

@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ServerProperties {

    private Integer port;

    private InetAddress address;

    @NestedConfigurationProperty
    private final ErrorProperties error = new ErrorProperties();

    private ForwardHeadersStrategy forwardHeadersStrategy = ForwardHeadersStrategy.NONE;

    private String serverHeader;

    private DataSize maxHttpHeaderSize = DataSize.ofKilobytes(8);

    private Duration connectionTimeout;

    @NestedConfigurationProperty
    private Ssl ssl;

    @NestedConfigurationProperty
    private final Compression compression = new Compression();

    @NestedConfigurationProperty
    private final Http2 http2 = new Http2();

    private final Servlet servlet = new Servlet();

    private final Tomcat tomcat = new Tomcat();

    private final Jetty jetty = new Jetty();

    private final Undertow undertow = new Undertow();

    public Integer getPort() {
        return this.port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    @DeprecatedConfigurationProperty(reason = "replaced to support additional strategies", replacement = "server.forward-headers-strategy")
    public Boolean isUseForwardHeaders() {
        return ForwardHeadersStrategy.NATIVE.equals(this.forwardHeadersStrategy);
    }

    public void setUseForwardHeaders(Boolean useForwardHeaders) {
        this.forwardHeadersStrategy = Boolean.TRUE.equals(useForwardHeaders) ? ForwardHeadersStrategy.NATIVE : ForwardHeadersStrategy.NONE;
    }

    public String getServerHeader() {
        return this.serverHeader;
    }

    public void setServerHeader(String serverHeader) {
        this.serverHeader = serverHeader;
    }

    public DataSize getMaxHttpHeaderSize() {
        return this.maxHttpHeaderSize;
    }

    public void setMaxHttpHeaderSize(DataSize maxHttpHeaderSize) {
        this.maxHttpHeaderSize = maxHttpHeaderSize;
    }

    public Duration getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public void setConnectionTimeout(Duration connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public ErrorProperties getError() {
        return this.error;
    }

    public Ssl getSsl() {
        return this.ssl;
    }

    public void setSsl(Ssl ssl) {
        this.ssl = ssl;
    }

    public Compression getCompression() {
        return this.compression;
    }

    public Http2 getHttp2() {
        return this.http2;
    }

    public Servlet getServlet() {
        return this.servlet;
    }

    public Tomcat getTomcat() {
        return this.tomcat;
    }

    public Jetty getJetty() {
        return this.jetty;
    }

    public Undertow getUndertow() {
        return this.undertow;
    }

    public ForwardHeadersStrategy getForwardHeadersStrategy() {
        return this.forwardHeadersStrategy;
    }

    public void setForwardHeadersStrategy(ForwardHeadersStrategy forwardHeadersStrategy) {
        this.forwardHeadersStrategy = forwardHeadersStrategy;
    }

    public static class Servlet {

        private final Map<String, String> contextParameters = new HashMap<>();

        private String contextPath;

        private String applicationDisplayName = "application";

        @NestedConfigurationProperty
        private final Jsp jsp = new Jsp();

        @NestedConfigurationProperty
        private final Session session = new Session();

        public String getContextPath() {
            return this.contextPath;
        }

        public void setContextPath(String contextPath) {
            this.contextPath = cleanContextPath(contextPath);
        }

        private String cleanContextPath(String contextPath) {
            String candidate = StringUtils.trimWhitespace(contextPath);
            if (StringUtils.hasText(candidate) && candidate.endsWith("/")) {
                return candidate.substring(0, candidate.length() - 1);
            }
            return candidate;
        }

        public String getApplicationDisplayName() {
            return this.applicationDisplayName;
        }

        public void setApplicationDisplayName(String displayName) {
            this.applicationDisplayName = displayName;
        }

        public Map<String, String> getContextParameters() {
            return this.contextParameters;
        }

        public Jsp getJsp() {
            return this.jsp;
        }

        public Session getSession() {
            return this.session;
        }

    }

    public static class Tomcat {

        private final Accesslog accesslog = new Accesslog();

        private String internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
                + "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
                + "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
                + "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
                + "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
                + "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" + "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" //
                + "0:0:0:0:0:0:0:1|::1";

        private String protocolHeader;

        private String protocolHeaderHttpsValue = "https";

        private String portHeader = "X-Forwarded-Port";

        private String remoteIpHeader;

        private File basedir;

        @DurationUnit(ChronoUnit.SECONDS)
        private Duration backgroundProcessorDelay = Duration.ofSeconds(10);

        private int maxThreads = 200;

        private int minSpareThreads = 10;

        private DataSize maxHttpPostSize = DataSize.ofMegabytes(2);

        private DataSize maxSwallowSize = DataSize.ofMegabytes(2);

        private Boolean redirectContextRoot = true;

        private Boolean useRelativeRedirects;

        private Charset uriEncoding = StandardCharsets.UTF_8;

        private int maxConnections = 10000;

        private int acceptCount = 100;

        private int processorCache = 200;

        private List<String> additionalTldSkipPatterns = new ArrayList<>();

        private final Resource resource = new Resource();

        public int getMaxThreads() {
            return this.maxThreads;
        }

        public void setMaxThreads(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        public int getMinSpareThreads() {
            return this.minSpareThreads;
        }

        public void setMinSpareThreads(int minSpareThreads) {
            this.minSpareThreads = minSpareThreads;
        }

        public DataSize getMaxHttpPostSize() {
            return this.maxHttpPostSize;
        }

        public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
            this.maxHttpPostSize = maxHttpPostSize;
        }

        public Accesslog getAccesslog() {
            return this.accesslog;
        }

        public Duration getBackgroundProcessorDelay() {
            return this.backgroundProcessorDelay;
        }

        public void setBackgroundProcessorDelay(Duration backgroundProcessorDelay) {
            this.backgroundProcessorDelay = backgroundProcessorDelay;
        }

        public File getBasedir() {
            return this.basedir;
        }

        public void setBasedir(File basedir) {
            this.basedir = basedir;
        }

        public String getInternalProxies() {
            return this.internalProxies;
        }

        public void setInternalProxies(String internalProxies) {
            this.internalProxies = internalProxies;
        }

        public String getProtocolHeader() {
            return this.protocolHeader;
        }

        public void setProtocolHeader(String protocolHeader) {
            this.protocolHeader = protocolHeader;
        }

        public String getProtocolHeaderHttpsValue() {
            return this.protocolHeaderHttpsValue;
        }

        public void setProtocolHeaderHttpsValue(String protocolHeaderHttpsValue) {
            this.protocolHeaderHttpsValue = protocolHeaderHttpsValue;
        }

        public String getPortHeader() {
            return this.portHeader;
        }

        public void setPortHeader(String portHeader) {
            this.portHeader = portHeader;
        }

        public Boolean getRedirectContextRoot() {
            return this.redirectContextRoot;
        }

        public void setRedirectContextRoot(Boolean redirectContextRoot) {
            this.redirectContextRoot = redirectContextRoot;
        }

        public Boolean getUseRelativeRedirects() {
            return this.useRelativeRedirects;
        }

        public void setUseRelativeRedirects(Boolean useRelativeRedirects) {
            this.useRelativeRedirects = useRelativeRedirects;
        }

        public String getRemoteIpHeader() {
            return this.remoteIpHeader;
        }

        public void setRemoteIpHeader(String remoteIpHeader) {
            this.remoteIpHeader = remoteIpHeader;
        }

        public Charset getUriEncoding() {
            return this.uriEncoding;
        }

        public void setUriEncoding(Charset uriEncoding) {
            this.uriEncoding = uriEncoding;
        }

        public int getMaxConnections() {
            return this.maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }

        public DataSize getMaxSwallowSize() {
            return this.maxSwallowSize;
        }

        public void setMaxSwallowSize(DataSize maxSwallowSize) {
            this.maxSwallowSize = maxSwallowSize;
        }

        public int getAcceptCount() {
            return this.acceptCount;
        }

        public void setAcceptCount(int acceptCount) {
            this.acceptCount = acceptCount;
        }

        public int getProcessorCache() {
            return this.processorCache;
        }

        public void setProcessorCache(int processorCache) {
            this.processorCache = processorCache;
        }

        public List<String> getAdditionalTldSkipPatterns() {
            return this.additionalTldSkipPatterns;
        }

        public void setAdditionalTldSkipPatterns(List<String> additionalTldSkipPatterns) {
            this.additionalTldSkipPatterns = additionalTldSkipPatterns;
        }

        public Resource getResource() {
            return this.resource;
        }

        public static class Accesslog {

            private boolean enabled = false;

            private String conditionIf;

            private String conditionUnless;

            private String pattern = "common";

            private String directory = "logs";

            protected String prefix = "access_log";

            private String suffix = ".log";

            private String encoding;

            private String locale;

            private boolean checkExists = false;

            private boolean rotate = true;

            private boolean renameOnRotate = false;

            private int maxDays = -1;

            private String fileDateFormat = ".yyyy-MM-dd";

            private boolean ipv6Canonical = false;

            private boolean requestAttributesEnabled = false;

            private boolean buffered = true;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getConditionIf() {
                return this.conditionIf;
            }

            public void setConditionIf(String conditionIf) {
                this.conditionIf = conditionIf;
            }

            public String getConditionUnless() {
                return this.conditionUnless;
            }

            public void setConditionUnless(String conditionUnless) {
                this.conditionUnless = conditionUnless;
            }

            public String getPattern() {
                return this.pattern;
            }

            public void setPattern(String pattern) {
                this.pattern = pattern;
            }

            public String getDirectory() {
                return this.directory;
            }

            public void setDirectory(String directory) {
                this.directory = directory;
            }

            public String getPrefix() {
                return this.prefix;
            }

            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }

            public String getSuffix() {
                return this.suffix;
            }

            public void setSuffix(String suffix) {
                this.suffix = suffix;
            }

            public String getEncoding() {
                return this.encoding;
            }

            public void setEncoding(String encoding) {
                this.encoding = encoding;
            }

            public String getLocale() {
                return this.locale;
            }

            public void setLocale(String locale) {
                this.locale = locale;
            }

            public boolean isCheckExists() {
                return this.checkExists;
            }

            public void setCheckExists(boolean checkExists) {
                this.checkExists = checkExists;
            }

            public boolean isRotate() {
                return this.rotate;
            }

            public void setRotate(boolean rotate) {
                this.rotate = rotate;
            }

            public boolean isRenameOnRotate() {
                return this.renameOnRotate;
            }

            public void setRenameOnRotate(boolean renameOnRotate) {
                this.renameOnRotate = renameOnRotate;
            }

            public int getMaxDays() {
                return this.maxDays;
            }

            public void setMaxDays(int maxDays) {
                this.maxDays = maxDays;
            }

            public String getFileDateFormat() {
                return this.fileDateFormat;
            }

            public void setFileDateFormat(String fileDateFormat) {
                this.fileDateFormat = fileDateFormat;
            }

            public boolean isIpv6Canonical() {
                return this.ipv6Canonical;
            }

            public void setIpv6Canonical(boolean ipv6Canonical) {
                this.ipv6Canonical = ipv6Canonical;
            }

            public boolean isRequestAttributesEnabled() {
                return this.requestAttributesEnabled;
            }

            public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
                this.requestAttributesEnabled = requestAttributesEnabled;
            }

            public boolean isBuffered() {
                return this.buffered;
            }

            public void setBuffered(boolean buffered) {
                this.buffered = buffered;
            }

        }

        public static class Resource {

            private boolean allowCaching = true;

            private Duration cacheTtl;

            public boolean isAllowCaching() {
                return this.allowCaching;
            }

            public void setAllowCaching(boolean allowCaching) {
                this.allowCaching = allowCaching;
            }

            public Duration getCacheTtl() {
                return this.cacheTtl;
            }

            public void setCacheTtl(Duration cacheTtl) {
                this.cacheTtl = cacheTtl;
            }

        }

    }

    public static class Jetty {

        private final Accesslog accesslog = new Accesslog();

        private DataSize maxHttpPostSize = DataSize.ofBytes(200000);

        private Integer acceptors = -1;

        private Integer selectors = -1;

        public Accesslog getAccesslog() {
            return this.accesslog;
        }

        public DataSize getMaxHttpPostSize() {
            return this.maxHttpPostSize;
        }

        public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
            this.maxHttpPostSize = maxHttpPostSize;
        }

        public Integer getAcceptors() {
            return this.acceptors;
        }

        public void setAcceptors(Integer acceptors) {
            this.acceptors = acceptors;
        }

        public Integer getSelectors() {
            return this.selectors;
        }

        public void setSelectors(Integer selectors) {
            this.selectors = selectors;
        }

        public static class Accesslog {

            private boolean enabled = false;

            private FORMAT format = FORMAT.NCSA;

            private String customFormat;

            private String filename;

            private String fileDateFormat;

            private int retentionPeriod = 31; // no days

            private boolean append;

            private List<String> ignorePaths;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public FORMAT getFormat() {
                return this.format;
            }

            public void setFormat(FORMAT format) {
                this.format = format;
            }

            public String getCustomFormat() {
                return this.customFormat;
            }

            public void setCustomFormat(String customFormat) {
                this.customFormat = customFormat;
            }

            public String getFilename() {
                return this.filename;
            }

            public void setFilename(String filename) {
                this.filename = filename;
            }

            public String getFileDateFormat() {
                return this.fileDateFormat;
            }

            public void setFileDateFormat(String fileDateFormat) {
                this.fileDateFormat = fileDateFormat;
            }

            public int getRetentionPeriod() {
                return this.retentionPeriod;
            }

            public void setRetentionPeriod(int retentionPeriod) {
                this.retentionPeriod = retentionPeriod;
            }

            public boolean isAppend() {
                return this.append;
            }

            public void setAppend(boolean append) {
                this.append = append;
            }

            public List<String> getIgnorePaths() {
                return this.ignorePaths;
            }

            public void setIgnorePaths(List<String> ignorePaths) {
                this.ignorePaths = ignorePaths;
            }

            public enum FORMAT {

                NCSA,

                EXTENDED_NCSA

            }

        }

    }

    public static class Undertow {

        private DataSize maxHttpPostSize = DataSize.ofBytes(-1);

        private DataSize bufferSize;

        private Integer ioThreads;

        private Integer workerThreads;

        private Boolean directBuffers;

        private boolean eagerFilterInit = true;

        // private int maxParameters = UndertowOptions.DEFAULT_MAX_PARAMETERS;
        private int maxParameters;

        // private int maxHeaders = UndertowOptions.DEFAULT_MAX_HEADERS;
        private int maxHeaders;

        private int maxCookies = 200;

        private boolean allowEncodedSlash = false;

        private boolean decodeUrl = true;

        private Charset urlCharset = StandardCharsets.UTF_8;

        private boolean alwaysSetKeepAlive = true;

        private final Accesslog accesslog = new Accesslog();

        public DataSize getMaxHttpPostSize() {
            return this.maxHttpPostSize;
        }

        public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
            this.maxHttpPostSize = maxHttpPostSize;
        }

        public DataSize getBufferSize() {
            return this.bufferSize;
        }

        public void setBufferSize(DataSize bufferSize) {
            this.bufferSize = bufferSize;
        }

        public Integer getIoThreads() {
            return this.ioThreads;
        }

        public void setIoThreads(Integer ioThreads) {
            this.ioThreads = ioThreads;
        }

        public Integer getWorkerThreads() {
            return this.workerThreads;
        }

        public void setWorkerThreads(Integer workerThreads) {
            this.workerThreads = workerThreads;
        }

        public Boolean getDirectBuffers() {
            return this.directBuffers;
        }

        public void setDirectBuffers(Boolean directBuffers) {
            this.directBuffers = directBuffers;
        }

        public boolean isEagerFilterInit() {
            return this.eagerFilterInit;
        }

        public void setEagerFilterInit(boolean eagerFilterInit) {
            this.eagerFilterInit = eagerFilterInit;
        }

        public int getMaxParameters() {
            return this.maxParameters;
        }

        public void setMaxParameters(Integer maxParameters) {
            this.maxParameters = maxParameters;
        }

        public int getMaxHeaders() {
            return this.maxHeaders;
        }

        public void setMaxHeaders(int maxHeaders) {
            this.maxHeaders = maxHeaders;
        }

        public Integer getMaxCookies() {
            return this.maxCookies;
        }

        public void setMaxCookies(Integer maxCookies) {
            this.maxCookies = maxCookies;
        }

        public boolean isAllowEncodedSlash() {
            return this.allowEncodedSlash;
        }

        public void setAllowEncodedSlash(boolean allowEncodedSlash) {
            this.allowEncodedSlash = allowEncodedSlash;
        }

        public boolean isDecodeUrl() {
            return this.decodeUrl;
        }

        public void setDecodeUrl(Boolean decodeUrl) {
            this.decodeUrl = decodeUrl;
        }

        public Charset getUrlCharset() {
            return this.urlCharset;
        }

        public void setUrlCharset(Charset urlCharset) {
            this.urlCharset = urlCharset;
        }

        public boolean isAlwaysSetKeepAlive() {
            return this.alwaysSetKeepAlive;
        }

        public void setAlwaysSetKeepAlive(boolean alwaysSetKeepAlive) {
            this.alwaysSetKeepAlive = alwaysSetKeepAlive;
        }

        public Accesslog getAccesslog() {
            return this.accesslog;
        }

        public static class Accesslog {

            private boolean enabled = false;

            private String pattern = "common";

            protected String prefix = "access_log.";

            private String suffix = "log";

            private File dir = new File("logs");

            private boolean rotate = true;

            public boolean isEnabled() {
                return this.enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getPattern() {
                return this.pattern;
            }

            public void setPattern(String pattern) {
                this.pattern = pattern;
            }

            public String getPrefix() {
                return this.prefix;
            }

            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }

            public String getSuffix() {
                return this.suffix;
            }

            public void setSuffix(String suffix) {
                this.suffix = suffix;
            }

            public File getDir() {
                return this.dir;
            }

            public void setDir(File dir) {
                this.dir = dir;
            }

            public boolean isRotate() {
                return this.rotate;
            }

            public void setRotate(boolean rotate) {
                this.rotate = rotate;
            }

        }

    }

    public enum ForwardHeadersStrategy {

        NATIVE,

        FRAMEWORK,

        NONE

    }

}
