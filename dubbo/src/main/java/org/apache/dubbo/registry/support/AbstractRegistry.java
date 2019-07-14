package org.apache.dubbo.registry.support;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.*;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.dubbo.common.constants.CommonConstants.*;
import static org.apache.dubbo.common.constants.RegistryConstants.*;

public abstract class AbstractRegistry implements Registry {

    private static final char URL_SEPARATOR = ' ';

    private static final String URL_SPLIT = "\\s+";

    private static final int MAX_RETRY_TIMES_SAVE_PROPERTIES = 3;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Properties properties = new Properties();

    private final ExecutorService registryCacheExecutor = Executors.newFixedThreadPool(1, new NamedThreadFactory("DubboSaveRegistryCache", true));

    private final boolean syncSaveFile;

    private final AtomicLong lastCacheChanged = new AtomicLong();

    private final AtomicInteger savePropertiesRetryTimes = new AtomicInteger();

    private final Set<URL> registered = new ConcurrentHashSet<>();

    private final ConcurrentMap<URL, Set<NotifyListener>> subscribed = new ConcurrentHashMap<>();

    private final ConcurrentMap<URL, Map<String, List<URL>>> notified = new ConcurrentHashMap<>();

    private URL registryUrl;

    private File file;

    public AbstractRegistry(URL url) {
        setUrl(url);
        syncSaveFile = url.getParameter(REGISTRY_FILESAVE_SYNC_KEY, false);
        String filename = url.getParameter(FILE_KEY, System.getProperty("user.home") + "/.dubbo/dubbo-registry-" + url.getParameter(APPLICATION_KEY) + "-" + url.getAddress() + ".cache");
        File file = null;
        if (ConfigUtils.isNotEmpty(filename)) {
            file = new File(filename);
            if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
                if (!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("Invalid registry cache file " + file + ", cause: Failed to create directory " + file.getParentFile() + "!");
                }
            }
        }
        this.file = file;
        loadProperties();
        notify(url.getBackupUrls());
    }

    protected static List<URL> filterEmpty(URL url, List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            List<URL> result = new ArrayList<>(1);
            result.add(url.setProtocol(EMPTY_PROTOCOL));
            return result;
        }
        return urls;
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    protected void setUrl(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("registry url == null");
        }
        this.registryUrl = url;
    }

    public Set<URL> getRegistered() {
        return Collections.unmodifiableSet(registered);
    }

    public Map<URL, Set<NotifyListener>> getSubscribed() {
        return Collections.unmodifiableMap(subscribed);
    }

    public Map<URL, Map<String, List<URL>>> getNotified() {
        return Collections.unmodifiableMap(notified);
    }

    public File getCacheFile() {
        return file;
    }

    public Properties getCacheProperties() {
        return properties;
    }

    public AtomicLong getLastCacheChanged() {
        return lastCacheChanged;
    }

    public void doSaveProperties(long version) {
        if (version < lastCacheChanged.get()) {
            return;
        }
        if (file == null) {
            return;
        }
        try {
            File lockfile = new File(file.getAbsolutePath() + ".lock");
            if (!lockfile.exists()) {
                lockfile.createNewFile();
            }
            try (RandomAccessFile raf = new RandomAccessFile(lockfile, "rw"); FileChannel channel = raf.getChannel()) {
                FileLock lock = channel.tryLock();
                if (lock == null) {
                    throw new IOException("Can not lock the registry cache file " + file.getAbsolutePath() + ", ignore and retry later, maybe multi java process use the file, please config: dubbo.registry.file=xxx.properties");
                }
                try {
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    try (FileOutputStream outputFile = new FileOutputStream(file)) {
                        properties.store(outputFile, "Dubbo Registry Cache");
                    }
                } finally {
                    lock.release();
                }
            }
        } catch (Throwable e) {
            savePropertiesRetryTimes.incrementAndGet();
            if (savePropertiesRetryTimes.get() >= MAX_RETRY_TIMES_SAVE_PROPERTIES) {
                logger.warn("Failed to save registry cache file after retrying " + MAX_RETRY_TIMES_SAVE_PROPERTIES + " times, cause: " + e.getMessage(), e);
                savePropertiesRetryTimes.set(0);
                return;
            }
            if (version < lastCacheChanged.get()) {
                savePropertiesRetryTimes.set(0);
                return;
            } else {
                registryCacheExecutor.execute(new SaveProperties(lastCacheChanged.incrementAndGet()));
            }
            logger.warn("Failed to save registry cache file, will retry, cause: " + e.getMessage(), e);
        }
    }

    private void loadProperties() {
        if (file != null && file.exists()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                properties.load(in);
                if (logger.isInfoEnabled()) {
                    // {
                    // org.apache.dubbo.demo.DemoService:1.0.0=
                    // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=6780&register=true&release=&revision=1.0.0&sayHello.timeout=1000&side=provider&timestamp=1562557776866&version=1.0.0
                    // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=9896&register=true&release=&revision=1.0.0&sayHello.timeout=1000&side=provider&timestamp=1562554187077&version=1.0.0
                    // }


                    // {
                    // org.apache.dubbo.demo.DemoService:1.0.0=empty://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&bind.ip=192.168.1.108&bind.port=20880&category=configurators&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=4068&register=true&release=&revision=1.0.0&side=provider&timestamp=1562324150390&version=1.0.0,
                    // org.apache.dubbo.demo.DemoService=empty://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&bind.ip=192.168.1.108&bind.port=20880&category=configurators&check=false&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=5792&register=true&release=&side=provider&timestamp=1562323683917
                    // }



                    // {
                    // org.apache.dubbo.demo.DemoService:1.0.0=empty://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer&category=routers&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=10648&revision=1.0.0&side=consumer&sticky=false&timestamp=1562324853108&version=1.0.0 empty://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer&category=configurators&dubbo=2.0.2&interface=org.apache.dubbo.demo.DemoService&lazy=false&methods=sayHello&pid=10648&revision=1.0.0&side=consumer&sticky=false&timestamp=1562324853108&version=1.0.0
                    // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=7504&register=true&release=&revision=1.0.0&side=provider&timestamp=1562323846894&version=1.0.0
                    // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-api-provider&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello&pid=4068&register=true&release=&revision=1.0.0&side=provider&timestamp=1562324150390&version=1.0.0
                    // }
                    logger.info("Load registry cache file " + file + ", data: " + properties);
                }
            } catch (Throwable e) {
                logger.warn("Failed to load registry cache file " + file, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        logger.warn(e.getMessage(), e);
                    }
                }
            }
        }
    }

    public List<URL> getCacheUrls(URL url) {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            if (key != null && key.length() > 0 && key.equals(url.getServiceKey()) && (Character.isLetter(key.charAt(0)) || key.charAt(0) == '_') && value != null && value.length() > 0) {
                String[] arr = value.trim().split(URL_SPLIT);
                List<URL> urls = new ArrayList<>();
                for (String u : arr) {
                    urls.add(URL.valueOf(u));
                }
                return urls;
            }
        }
        return null;
    }

    @Override
    public List<URL> lookup(URL url) {
        List<URL> result = new ArrayList<>();
        Map<String, List<URL>> notifiedUrls = getNotified().get(url);
        if (notifiedUrls != null && notifiedUrls.size() > 0) {
            for (List<URL> urls : notifiedUrls.values()) {
                for (URL u : urls) {
                    if (!EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        } else {
            final AtomicReference<List<URL>> reference = new AtomicReference<>();
            NotifyListener listener = reference::set;
            subscribe(url, listener);
            List<URL> urls = reference.get();
            if (CollectionUtils.isNotEmpty(urls)) {
                for (URL u : urls) {
                    if (!EMPTY_PROTOCOL.equals(u.getProtocol())) {
                        result.add(u);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void register(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("register url == null");
        }
        if (logger.isInfoEnabled()) {
            // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
            // &application=dubbo-demo-api-provider
            // &deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &methods=sayHello
            // &pid=6780
            // &register=true
            // &release=
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=provider
            // &timestamp=1562557776866
            // &version=1.0.0


            // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer
            // &category=consumers
            // &check=false
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.demo.DemoService
            // &lazy=false
            // &methods=sayHello
            // &pid=6220&revision=1.0.0
            // &side=consumer
            // &sticky=false
            // &timestamp=1562560440800
            // &version=1.0.0
            logger.info("Register: " + url);
        }
        registered.add(url);
    }

    @Override
    public void unregister(URL url) {
        if (url == null) {
            throw new IllegalArgumentException("unregister url == null");
        }
        if (logger.isInfoEnabled()) {
            // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer
            // &category=consumers&check=false
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.demo.DemoService
            // &lazy=false
            // &methods=sayHello
            // &pid=6220
            // &revision=1.0.0
            // &side=consumer
            // &sticky=false
            // &timestamp=1562560440800
            // &version=1.0.0

            // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
            // &application=dubbo-demo-api-provider
            // &deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &methods=sayHello
            // &pid=4744
            // &register=true
            // &release=
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=provider
            // &timestamp=1562563351261
            // &version=1.0.0
            logger.info("Unregister: " + url);
        }
        registered.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("subscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("subscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer
            // &category=providers,configurators,routers
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.demo.DemoService
            // &lazy=false
            // &methods=sayHello
            // &pid=6220
            // &revision=1.0.0
            // &side=consumer
            // &sticky=false
            // &timestamp=1562560440800
            // &version=1.0.0


            // provider://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
            // &application=dubbo-demo-api-provider
            // &bind.ip=192.168.1.108
            // &bind.port=20880
            // &category=configurators
            // &check=false
            // &deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &methods=sayHello
            // &pid=6780
            // &register=true
            // &release=
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=provider
            // &timestamp=1562557776866
            // &version=1.0.0
            logger.info("Subscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.computeIfAbsent(url, n -> new ConcurrentHashSet<>());
        listeners.add(listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null) {
            throw new IllegalArgumentException("unsubscribe url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("unsubscribe listener == null");
        }
        if (logger.isInfoEnabled()) {
            // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer
            // &category=providers,configurators,routers
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.demo.DemoService
            // &lazy=false
            // &methods=sayHello
            // &pid=6220
            // &revision=1.0.0
            // &side=consumer
            // &sticky=false
            // &timestamp=1562560440800
            // &version=1.0.0


            // provider://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
            // &application=dubbo-demo-api-provider
            // &bind.ip=192.168.1.108
            // &bind.port=20880
            // &category=configurators
            // &check=false
            // &deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &methods=sayHello
            // &pid=4744
            // &register=true
            // &release=
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=provider
            // &timestamp=1562563351261
            // &version=1.0.0
            logger.info("Unsubscribe: " + url);
        }
        Set<NotifyListener> listeners = subscribed.get(url);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    protected void recover() throws Exception {
        Set<URL> recoverRegistered = new HashSet<>(getRegistered());
        if (!recoverRegistered.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover register url " + recoverRegistered);
            }
            for (URL url : recoverRegistered) {
                register(url);
            }
        }
        Map<URL, Set<NotifyListener>> recoverSubscribed = new HashMap<>(getSubscribed());
        if (!recoverSubscribed.isEmpty()) {
            if (logger.isInfoEnabled()) {
                logger.info("Recover subscribe url " + recoverSubscribed.keySet());
            }
            for (Map.Entry<URL, Set<NotifyListener>> entry : recoverSubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    subscribe(url, listener);
                }
            }
        }
    }

    protected void notify(List<URL> urls) {
        if (CollectionUtils.isEmpty(urls)) {
            return;
        }
        for (Map.Entry<URL, Set<NotifyListener>> entry : getSubscribed().entrySet()) {
            URL url = entry.getKey();
            if (!UrlUtils.isMatch(url, urls.get(0))) {
                continue;
            }
            Set<NotifyListener> listeners = entry.getValue();
            if (listeners != null) {
                for (NotifyListener listener : listeners) {
                    try {
                        notify(url, listener, filterEmpty(url, urls));
                    } catch (Throwable t) {
                        logger.error("Failed to notify registry event, urls: " + urls + ", cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    protected void notify(URL url, NotifyListener listener, List<URL> urls) {
        if (url == null) {
            throw new IllegalArgumentException("notify url == null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("notify listener == null");
        }
        if ((CollectionUtils.isEmpty(urls)) && !ANY_VALUE.equals(url.getServiceInterface())) {
            // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer
            // &category=providers,configurators,routers
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.demo.DemoService
            // &lazy=false
            // &methods=sayHello
            // &pid=6220
            // &revision=1.0.0
            // &side=consumer
            // &sticky=false
            // &timestamp=1562560440800
            // &version=1.0.0,
            //
            // urls: [
            // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
            // &application=dubbo-demo-api-provider
            // &deprecated=false
            // &dubbo=2.0.2
            // &dynamic=true
            // &generic=false
            // &interface=org.apache.dubbo.demo.DemoService
            // &methods=sayHello
            // &pid=6780
            // &register=true
            // &release=
            // &revision=1.0.0
            // &sayHello.timeout=1000
            // &side=provider
            // &timestamp=1562557776866
            // &version=1.0.0
            // ]
            logger.warn("Ignore empty notify urls for subscribe url " + url);
            return;
        }
        if (logger.isInfoEnabled()) {
            logger.info("Notify urls for subscribe url " + url + ", urls: " + urls);
        }
        Map<String, List<URL>> result = new HashMap<>();
        for (URL u : urls) {
            // 判断是否合适
            if (UrlUtils.isMatch(url, u)) {
                String category = u.getParameter(CATEGORY_KEY, DEFAULT_CATEGORY);
                List<URL> categoryList = result.computeIfAbsent(category, k -> new ArrayList<>());
                categoryList.add(u);
            }
        }
        if (result.size() == 0) {
            return;
        }
        Map<String, List<URL>> categoryNotified = notified.computeIfAbsent(url, u -> new ConcurrentHashMap<>());
        for (Map.Entry<String, List<URL>> entry : result.entrySet()) {
            String category = entry.getKey();
            List<URL> categoryList = entry.getValue();
            categoryNotified.put(category, categoryList);
            listener.notify(categoryList);
            saveProperties(url);
        }
    }

    private void saveProperties(URL url) {
        if (file == null) {
            return;
        }
        try {
            StringBuilder buf = new StringBuilder();
            Map<String, List<URL>> categoryNotified = notified.get(url);
            if (categoryNotified != null) {
                for (List<URL> us : categoryNotified.values()) {
                    for (URL u : us) {
                        if (buf.length() > 0) {
                            buf.append(URL_SEPARATOR);
                        }
                        buf.append(u.toFullString());
                    }
                }
            }
            properties.setProperty(url.getServiceKey(), buf.toString());
            long version = lastCacheChanged.incrementAndGet();
            if (syncSaveFile) {
                doSaveProperties(version);
            } else {
                registryCacheExecutor.execute(new SaveProperties(version));
            }
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
        }
    }

    @Override
    public void destroy() {
        if (logger.isInfoEnabled()) {
            // redis://127.0.0.1:6379/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-consumer
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.registry.RegistryService
            // &pid=6220
            // &timestamp=1562560441186

            // redis://127.0.0.1:6379/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-api-provider
            // &dubbo=2.0.2
            // &interface=org.apache.dubbo.registry.RegistryService
            // &pid=4744
            // &timestamp=1562563351247
            logger.info("Destroy registry:" + getUrl());
        }
        Set<URL> destroyRegistered = new HashSet<>(getRegistered());
        if (!destroyRegistered.isEmpty()) {
            for (URL url : new HashSet<>(getRegistered())) {
                if (url.getParameter(DYNAMIC_KEY, true)) {
                    try {
                        unregister(url);
                        if (logger.isInfoEnabled()) {
                            // dubbo://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
                            // &application=dubbo-demo-api-provider
                            // &deprecated=false
                            // &dubbo=2.0.2
                            // &dynamic=true
                            // &generic=false
                            // &interface=org.apache.dubbo.demo.DemoService
                            // &methods=sayHello
                            // &pid=4744&register=true
                            // &release=
                            // &revision=1.0.0
                            // &sayHello.timeout=1000
                            // &side=provider
                            // &timestamp=1562563351261
                            // &version=1.0.0


                            // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer
                            // &category=consumers
                            // &check=false
                            // &dubbo=2.0.2
                            // &interface=org.apache.dubbo.demo.DemoService
                            // &lazy=false
                            // &methods=sayHello
                            // &pid=6220
                            // &revision=1.0.0
                            // &side=consumer
                            // &sticky=false
                            // &timestamp=1562560440800
                            // &version=1.0.0
                            logger.info("Destroy unregister url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unregister url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
        Map<URL, Set<NotifyListener>> destroySubscribed = new HashMap<>(getSubscribed());
        if (!destroySubscribed.isEmpty()) {
            for (Map.Entry<URL, Set<NotifyListener>> entry : destroySubscribed.entrySet()) {
                URL url = entry.getKey();
                for (NotifyListener listener : entry.getValue()) {
                    try {
                        unsubscribe(url, listener);
                        if (logger.isInfoEnabled()) {
                            // consumer://192.168.1.108/org.apache.dubbo.demo.DemoService?application=dubbo-demo-api-consumer
                            // &category=providers,configurators,routers
                            // &dubbo=2.0.2
                            // &interface=org.apache.dubbo.demo.DemoService
                            // &lazy=false
                            // &methods=sayHello
                            // &pid=6220
                            // &revision=1.0.0
                            // &side=consumer
                            // &sticky=false
                            // &timestamp=1562560440800
                            // &version=1.0.0

                            // provider://192.168.1.108:20880/org.apache.dubbo.demo.DemoService?anyhost=true
                            // &application=dubbo-demo-api-provider
                            // &bind.ip=192.168.1.108
                            // &bind.port=20880
                            // &category=configurators
                            // &check=false
                            // &deprecated=false
                            // &dubbo=2.0.2
                            // &dynamic=true
                            // &generic=false
                            // &interface=org.apache.dubbo.demo.DemoService
                            // &methods=sayHello
                            // &pid=4744
                            // &register=true
                            // &release=
                            // &revision=1.0.0
                            // &sayHello.timeout=1000
                            // &side=provider
                            // &timestamp=1562563351261
                            // &version=1.0.0
                            logger.info("Destroy unsubscribe url " + url);
                        }
                    } catch (Throwable t) {
                        logger.warn("Failed to unsubscribe url " + url + " to registry " + getUrl() + " on destroy, cause: " + t.getMessage(), t);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return getUrl().toString();
    }

    private class SaveProperties implements Runnable {
        private long version;

        private SaveProperties(long version) {
            this.version = version;
        }

        @Override
        public void run() {
            doSaveProperties(version);
        }

    }

}
