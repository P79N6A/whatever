package org.springframework.context.support;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceBundleMessageSource extends AbstractResourceBasedMessageSource implements BeanClassLoaderAware {

    @Nullable
    private ClassLoader bundleClassLoader;

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    private final Map<String, Map<Locale, ResourceBundle>> cachedResourceBundles = new ConcurrentHashMap<>();

    private final Map<ResourceBundle, Map<String, Map<Locale, MessageFormat>>> cachedBundleMessageFormats = new ConcurrentHashMap<>();

    @Nullable
    private volatile MessageSourceControl control = new MessageSourceControl();

    public ResourceBundleMessageSource() {
        setDefaultEncoding("ISO-8859-1");
    }

    public void setBundleClassLoader(ClassLoader classLoader) {
        this.bundleClassLoader = classLoader;
    }

    @Nullable
    protected ClassLoader getBundleClassLoader() {
        return (this.bundleClassLoader != null ? this.bundleClassLoader : this.beanClassLoader);
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        Set<String> basenames = getBasenameSet();
        for (String basename : basenames) {
            ResourceBundle bundle = getResourceBundle(basename, locale);
            if (bundle != null) {
                String result = getStringOrNull(bundle, code);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    protected MessageFormat resolveCode(String code, Locale locale) {
        Set<String> basenames = getBasenameSet();
        for (String basename : basenames) {
            ResourceBundle bundle = getResourceBundle(basename, locale);
            if (bundle != null) {
                MessageFormat messageFormat = getMessageFormat(bundle, code, locale);
                if (messageFormat != null) {
                    return messageFormat;
                }
            }
        }
        return null;
    }

    @Nullable
    protected ResourceBundle getResourceBundle(String basename, Locale locale) {
        if (getCacheMillis() >= 0) {
            // Fresh ResourceBundle.getBundle call in order to let ResourceBundle
            // do its native caching, at the expense of more extensive lookup steps.
            return doGetBundle(basename, locale);
        } else {
            // Cache forever: prefer locale cache over repeated getBundle calls.
            Map<Locale, ResourceBundle> localeMap = this.cachedResourceBundles.get(basename);
            if (localeMap != null) {
                ResourceBundle bundle = localeMap.get(locale);
                if (bundle != null) {
                    return bundle;
                }
            }
            try {
                ResourceBundle bundle = doGetBundle(basename, locale);
                if (localeMap == null) {
                    localeMap = new ConcurrentHashMap<>();
                    Map<Locale, ResourceBundle> existing = this.cachedResourceBundles.putIfAbsent(basename, localeMap);
                    if (existing != null) {
                        localeMap = existing;
                    }
                }
                localeMap.put(locale, bundle);
                return bundle;
            } catch (MissingResourceException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("ResourceBundle [" + basename + "] not found for MessageSource: " + ex.getMessage());
                }
                // Assume bundle not found
                // -> do NOT throw the exception to allow for checking parent message source.
                return null;
            }
        }
    }

    protected ResourceBundle doGetBundle(String basename, Locale locale) throws MissingResourceException {
        ClassLoader classLoader = getBundleClassLoader();
        Assert.state(classLoader != null, "No bundle ClassLoader set");
        MessageSourceControl control = this.control;
        if (control != null) {
            try {
                return ResourceBundle.getBundle(basename, locale, classLoader, control);
            } catch (UnsupportedOperationException ex) {
                // Probably in a Jigsaw environment on JDK 9+
                this.control = null;
                String encoding = getDefaultEncoding();
                if (encoding != null && logger.isInfoEnabled()) {
                    logger.info("ResourceBundleMessageSource is configured to read resources with encoding '" + encoding + "' but ResourceBundle.Control not supported in current system environment: " + ex.getMessage() + " - falling back to plain ResourceBundle.getBundle retrieval with the " + "platform default encoding. Consider setting the 'defaultEncoding' property to 'null' " + "for participating in the platform default and therefore avoiding this log message.");
                }
            }
        }
        // Fallback: plain getBundle lookup without Control handle
        return ResourceBundle.getBundle(basename, locale, classLoader);
    }

    protected ResourceBundle loadBundle(Reader reader) throws IOException {
        return new PropertyResourceBundle(reader);
    }

    protected ResourceBundle loadBundle(InputStream inputStream) throws IOException {
        return new PropertyResourceBundle(inputStream);
    }

    @Nullable
    protected MessageFormat getMessageFormat(ResourceBundle bundle, String code, Locale locale) throws MissingResourceException {
        Map<String, Map<Locale, MessageFormat>> codeMap = this.cachedBundleMessageFormats.get(bundle);
        Map<Locale, MessageFormat> localeMap = null;
        if (codeMap != null) {
            localeMap = codeMap.get(code);
            if (localeMap != null) {
                MessageFormat result = localeMap.get(locale);
                if (result != null) {
                    return result;
                }
            }
        }
        String msg = getStringOrNull(bundle, code);
        if (msg != null) {
            if (codeMap == null) {
                codeMap = new ConcurrentHashMap<>();
                Map<String, Map<Locale, MessageFormat>> existing = this.cachedBundleMessageFormats.putIfAbsent(bundle, codeMap);
                if (existing != null) {
                    codeMap = existing;
                }
            }
            if (localeMap == null) {
                localeMap = new ConcurrentHashMap<>();
                Map<Locale, MessageFormat> existing = codeMap.putIfAbsent(code, localeMap);
                if (existing != null) {
                    localeMap = existing;
                }
            }
            MessageFormat result = createMessageFormat(msg, locale);
            localeMap.put(locale, result);
            return result;
        }
        return null;
    }

    @Nullable
    protected String getStringOrNull(ResourceBundle bundle, String key) {
        if (bundle.containsKey(key)) {
            try {
                return bundle.getString(key);
            } catch (MissingResourceException ex) {
                // Assume key not found for some other reason
                // -> do NOT throw the exception to allow for checking parent message source.
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": basenames=" + getBasenameSet();
    }

    private class MessageSourceControl extends ResourceBundle.Control {

        @Override
        @Nullable
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            // Special handling of default encoding
            if (format.equals("java.properties")) {
                String bundleName = toBundleName(baseName, locale);
                final String resourceName = toResourceName(bundleName, "properties");
                final ClassLoader classLoader = loader;
                final boolean reloadFlag = reload;
                InputStream inputStream;
                try {
                    inputStream = AccessController.doPrivileged((PrivilegedExceptionAction<InputStream>) () -> {
                        InputStream is = null;
                        if (reloadFlag) {
                            URL url = classLoader.getResource(resourceName);
                            if (url != null) {
                                URLConnection connection = url.openConnection();
                                if (connection != null) {
                                    connection.setUseCaches(false);
                                    is = connection.getInputStream();
                                }
                            }
                        } else {
                            is = classLoader.getResourceAsStream(resourceName);
                        }
                        return is;
                    });
                } catch (PrivilegedActionException ex) {
                    throw (IOException) ex.getException();
                }
                if (inputStream != null) {
                    String encoding = getDefaultEncoding();
                    if (encoding != null) {
                        try (InputStreamReader bundleReader = new InputStreamReader(inputStream, encoding)) {
                            return loadBundle(bundleReader);
                        }
                    } else {
                        try (InputStream bundleStream = inputStream) {
                            return loadBundle(bundleStream);
                        }
                    }
                } else {
                    return null;
                }
            } else {
                // Delegate handling of "java.class" format to standard Control
                return super.newBundle(baseName, locale, format, loader, reload);
            }
        }

        @Override
        @Nullable
        public Locale getFallbackLocale(String baseName, Locale locale) {
            return (isFallbackToSystemLocale() ? super.getFallbackLocale(baseName, locale) : null);
        }

        @Override
        public long getTimeToLive(String baseName, Locale locale) {
            long cacheMillis = getCacheMillis();
            return (cacheMillis >= 0 ? cacheMillis : super.getTimeToLive(baseName, locale));
        }

        @Override
        public boolean needsReload(String baseName, Locale locale, String format, ClassLoader loader, ResourceBundle bundle, long loadTime) {
            if (super.needsReload(baseName, locale, format, loader, bundle, loadTime)) {
                cachedBundleMessageFormats.remove(bundle);
                return true;
            } else {
                return false;
            }
        }

    }

}
