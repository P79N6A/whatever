package org.springframework.context.support;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class ReloadableResourceBundleMessageSource extends AbstractResourceBasedMessageSource implements ResourceLoaderAware {

    private static final String PROPERTIES_SUFFIX = ".properties";

    private static final String XML_SUFFIX = ".xml";

    @Nullable
    private Properties fileEncodings;

    private boolean concurrentRefresh = true;

    private PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

    private ResourceLoader resourceLoader = new DefaultResourceLoader();

    // Cache to hold filename lists per Locale
    private final ConcurrentMap<String, Map<Locale, List<String>>> cachedFilenames = new ConcurrentHashMap<>();

    // Cache to hold already loaded properties per filename
    private final ConcurrentMap<String, PropertiesHolder> cachedProperties = new ConcurrentHashMap<>();

    // Cache to hold already loaded properties per filename
    private final ConcurrentMap<Locale, PropertiesHolder> cachedMergedProperties = new ConcurrentHashMap<>();

    public void setFileEncodings(Properties fileEncodings) {
        this.fileEncodings = fileEncodings;
    }

    public void setConcurrentRefresh(boolean concurrentRefresh) {
        this.concurrentRefresh = concurrentRefresh;
    }

    public void setPropertiesPersister(@Nullable PropertiesPersister propertiesPersister) {
        this.propertiesPersister = (propertiesPersister != null ? propertiesPersister : new DefaultPropertiesPersister());
    }

    @Override
    public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {
        this.resourceLoader = (resourceLoader != null ? resourceLoader : new DefaultResourceLoader());
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        if (getCacheMillis() < 0) {
            PropertiesHolder propHolder = getMergedProperties(locale);
            String result = propHolder.getProperty(code);
            if (result != null) {
                return result;
            }
        } else {
            for (String basename : getBasenameSet()) {
                List<String> filenames = calculateAllFilenames(basename, locale);
                for (String filename : filenames) {
                    PropertiesHolder propHolder = getProperties(filename);
                    String result = propHolder.getProperty(code);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    @Override
    @Nullable
    protected MessageFormat resolveCode(String code, Locale locale) {
        if (getCacheMillis() < 0) {
            PropertiesHolder propHolder = getMergedProperties(locale);
            MessageFormat result = propHolder.getMessageFormat(code, locale);
            if (result != null) {
                return result;
            }
        } else {
            for (String basename : getBasenameSet()) {
                List<String> filenames = calculateAllFilenames(basename, locale);
                for (String filename : filenames) {
                    PropertiesHolder propHolder = getProperties(filename);
                    MessageFormat result = propHolder.getMessageFormat(code, locale);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }

    protected PropertiesHolder getMergedProperties(Locale locale) {
        PropertiesHolder mergedHolder = this.cachedMergedProperties.get(locale);
        if (mergedHolder != null) {
            return mergedHolder;
        }
        Properties mergedProps = newProperties();
        long latestTimestamp = -1;
        String[] basenames = StringUtils.toStringArray(getBasenameSet());
        for (int i = basenames.length - 1; i >= 0; i--) {
            List<String> filenames = calculateAllFilenames(basenames[i], locale);
            for (int j = filenames.size() - 1; j >= 0; j--) {
                String filename = filenames.get(j);
                PropertiesHolder propHolder = getProperties(filename);
                if (propHolder.getProperties() != null) {
                    mergedProps.putAll(propHolder.getProperties());
                    if (propHolder.getFileTimestamp() > latestTimestamp) {
                        latestTimestamp = propHolder.getFileTimestamp();
                    }
                }
            }
        }
        mergedHolder = new PropertiesHolder(mergedProps, latestTimestamp);
        PropertiesHolder existing = this.cachedMergedProperties.putIfAbsent(locale, mergedHolder);
        if (existing != null) {
            mergedHolder = existing;
        }
        return mergedHolder;
    }

    protected List<String> calculateAllFilenames(String basename, Locale locale) {
        Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
        if (localeMap != null) {
            List<String> filenames = localeMap.get(locale);
            if (filenames != null) {
                return filenames;
            }
        }
        List<String> filenames = new ArrayList<>(7);
        filenames.addAll(calculateFilenamesForLocale(basename, locale));
        if (isFallbackToSystemLocale() && !locale.equals(Locale.getDefault())) {
            List<String> fallbackFilenames = calculateFilenamesForLocale(basename, Locale.getDefault());
            for (String fallbackFilename : fallbackFilenames) {
                if (!filenames.contains(fallbackFilename)) {
                    // Entry for fallback locale that isn't already in filenames list.
                    filenames.add(fallbackFilename);
                }
            }
        }
        filenames.add(basename);
        if (localeMap == null) {
            localeMap = new ConcurrentHashMap<>();
            Map<Locale, List<String>> existing = this.cachedFilenames.putIfAbsent(basename, localeMap);
            if (existing != null) {
                localeMap = existing;
            }
        }
        localeMap.put(locale, filenames);
        return filenames;
    }

    protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
        List<String> result = new ArrayList<>(3);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        StringBuilder temp = new StringBuilder(basename);
        temp.append('_');
        if (language.length() > 0) {
            temp.append(language);
            result.add(0, temp.toString());
        }
        temp.append('_');
        if (country.length() > 0) {
            temp.append(country);
            result.add(0, temp.toString());
        }
        if (variant.length() > 0 && (language.length() > 0 || country.length() > 0)) {
            temp.append('_').append(variant);
            result.add(0, temp.toString());
        }
        return result;
    }

    protected PropertiesHolder getProperties(String filename) {
        PropertiesHolder propHolder = this.cachedProperties.get(filename);
        long originalTimestamp = -2;
        if (propHolder != null) {
            originalTimestamp = propHolder.getRefreshTimestamp();
            if (originalTimestamp == -1 || originalTimestamp > System.currentTimeMillis() - getCacheMillis()) {
                // Up to date
                return propHolder;
            }
        } else {
            propHolder = new PropertiesHolder();
            PropertiesHolder existingHolder = this.cachedProperties.putIfAbsent(filename, propHolder);
            if (existingHolder != null) {
                propHolder = existingHolder;
            }
        }
        // At this point, we need to refresh...
        if (this.concurrentRefresh && propHolder.getRefreshTimestamp() >= 0) {
            // A populated but stale holder -> could keep using it.
            if (!propHolder.refreshLock.tryLock()) {
                // Getting refreshed by another thread already ->
                // let's return the existing properties for the time being.
                return propHolder;
            }
        } else {
            propHolder.refreshLock.lock();
        }
        try {
            PropertiesHolder existingHolder = this.cachedProperties.get(filename);
            if (existingHolder != null && existingHolder.getRefreshTimestamp() > originalTimestamp) {
                return existingHolder;
            }
            return refreshProperties(filename, propHolder);
        } finally {
            propHolder.refreshLock.unlock();
        }
    }

    protected PropertiesHolder refreshProperties(String filename, @Nullable PropertiesHolder propHolder) {
        long refreshTimestamp = (getCacheMillis() < 0 ? -1 : System.currentTimeMillis());
        Resource resource = this.resourceLoader.getResource(filename + PROPERTIES_SUFFIX);
        if (!resource.exists()) {
            resource = this.resourceLoader.getResource(filename + XML_SUFFIX);
        }
        if (resource.exists()) {
            long fileTimestamp = -1;
            if (getCacheMillis() >= 0) {
                // Last-modified timestamp of file will just be read if caching with timeout.
                try {
                    fileTimestamp = resource.lastModified();
                    if (propHolder != null && propHolder.getFileTimestamp() == fileTimestamp) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Re-caching properties for filename [" + filename + "] - file hasn't been modified");
                        }
                        propHolder.setRefreshTimestamp(refreshTimestamp);
                        return propHolder;
                    }
                } catch (IOException ex) {
                    // Probably a class path resource: cache it forever.
                    if (logger.isDebugEnabled()) {
                        logger.debug(resource + " could not be resolved in the file system - assuming that it hasn't changed", ex);
                    }
                    fileTimestamp = -1;
                }
            }
            try {
                Properties props = loadProperties(resource, filename);
                propHolder = new PropertiesHolder(props, fileTimestamp);
            } catch (IOException ex) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Could not parse properties file [" + resource.getFilename() + "]", ex);
                }
                // Empty holder representing "not valid".
                propHolder = new PropertiesHolder();
            }
        } else {
            // Resource does not exist.
            if (logger.isDebugEnabled()) {
                logger.debug("No properties file found for [" + filename + "] - neither plain properties nor XML");
            }
            // Empty holder representing "not found".
            propHolder = new PropertiesHolder();
        }
        propHolder.setRefreshTimestamp(refreshTimestamp);
        this.cachedProperties.put(filename, propHolder);
        return propHolder;
    }

    protected Properties loadProperties(Resource resource, String filename) throws IOException {
        Properties props = newProperties();
        try (InputStream is = resource.getInputStream()) {
            String resourceFilename = resource.getFilename();
            if (resourceFilename != null && resourceFilename.endsWith(XML_SUFFIX)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Loading properties [" + resource.getFilename() + "]");
                }
                this.propertiesPersister.loadFromXml(props, is);
            } else {
                String encoding = null;
                if (this.fileEncodings != null) {
                    encoding = this.fileEncodings.getProperty(filename);
                }
                if (encoding == null) {
                    encoding = getDefaultEncoding();
                }
                if (encoding != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Loading properties [" + resource.getFilename() + "] with encoding '" + encoding + "'");
                    }
                    this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Loading properties [" + resource.getFilename() + "]");
                    }
                    this.propertiesPersister.load(props, is);
                }
            }
            return props;
        }
    }

    protected Properties newProperties() {
        return new Properties();
    }

    public void clearCache() {
        logger.debug("Clearing entire resource bundle cache");
        this.cachedProperties.clear();
        this.cachedMergedProperties.clear();
    }

    public void clearCacheIncludingAncestors() {
        clearCache();
        if (getParentMessageSource() instanceof ReloadableResourceBundleMessageSource) {
            ((ReloadableResourceBundleMessageSource) getParentMessageSource()).clearCacheIncludingAncestors();
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + ": basenames=" + getBasenameSet();
    }

    protected class PropertiesHolder {

        @Nullable
        private final Properties properties;

        private final long fileTimestamp;

        private volatile long refreshTimestamp = -2;

        private final ReentrantLock refreshLock = new ReentrantLock();

        private final ConcurrentMap<String, Map<Locale, MessageFormat>> cachedMessageFormats = new ConcurrentHashMap<>();

        public PropertiesHolder() {
            this.properties = null;
            this.fileTimestamp = -1;
        }

        public PropertiesHolder(Properties properties, long fileTimestamp) {
            this.properties = properties;
            this.fileTimestamp = fileTimestamp;
        }

        @Nullable
        public Properties getProperties() {
            return this.properties;
        }

        public long getFileTimestamp() {
            return this.fileTimestamp;
        }

        public void setRefreshTimestamp(long refreshTimestamp) {
            this.refreshTimestamp = refreshTimestamp;
        }

        public long getRefreshTimestamp() {
            return this.refreshTimestamp;
        }

        @Nullable
        public String getProperty(String code) {
            if (this.properties == null) {
                return null;
            }
            return this.properties.getProperty(code);
        }

        @Nullable
        public MessageFormat getMessageFormat(String code, Locale locale) {
            if (this.properties == null) {
                return null;
            }
            Map<Locale, MessageFormat> localeMap = this.cachedMessageFormats.get(code);
            if (localeMap != null) {
                MessageFormat result = localeMap.get(locale);
                if (result != null) {
                    return result;
                }
            }
            String msg = this.properties.getProperty(code);
            if (msg != null) {
                if (localeMap == null) {
                    localeMap = new ConcurrentHashMap<>();
                    Map<Locale, MessageFormat> existing = this.cachedMessageFormats.putIfAbsent(code, localeMap);
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

    }

}
