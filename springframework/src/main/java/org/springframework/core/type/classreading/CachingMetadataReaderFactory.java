package org.springframework.core.type.classreading;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class CachingMetadataReaderFactory extends SimpleMetadataReaderFactory {

    public static final int DEFAULT_CACHE_LIMIT = 256;

    @Nullable
    private Map<Resource, MetadataReader> metadataReaderCache;

    public CachingMetadataReaderFactory() {
        super();
        setCacheLimit(DEFAULT_CACHE_LIMIT);
    }

    public CachingMetadataReaderFactory(@Nullable ClassLoader classLoader) {
        super(classLoader);
        setCacheLimit(DEFAULT_CACHE_LIMIT);
    }

    public CachingMetadataReaderFactory(@Nullable ResourceLoader resourceLoader) {
        super(resourceLoader);
        if (resourceLoader instanceof DefaultResourceLoader) {
            this.metadataReaderCache = ((DefaultResourceLoader) resourceLoader).getResourceCache(MetadataReader.class);
        } else {
            setCacheLimit(DEFAULT_CACHE_LIMIT);
        }
    }

    public void setCacheLimit(int cacheLimit) {
        if (cacheLimit <= 0) {
            this.metadataReaderCache = null;
        } else if (this.metadataReaderCache instanceof LocalResourceCache) {
            ((LocalResourceCache) this.metadataReaderCache).setCacheLimit(cacheLimit);
        } else {
            this.metadataReaderCache = new LocalResourceCache(cacheLimit);
        }
    }

    public int getCacheLimit() {
        if (this.metadataReaderCache instanceof LocalResourceCache) {
            return ((LocalResourceCache) this.metadataReaderCache).getCacheLimit();
        } else {
            return (this.metadataReaderCache != null ? Integer.MAX_VALUE : 0);
        }
    }

    @Override
    public MetadataReader getMetadataReader(Resource resource) throws IOException {
        if (this.metadataReaderCache instanceof ConcurrentMap) {
            // No synchronization necessary...
            MetadataReader metadataReader = this.metadataReaderCache.get(resource);
            if (metadataReader == null) {
                metadataReader = super.getMetadataReader(resource);
                this.metadataReaderCache.put(resource, metadataReader);
            }
            return metadataReader;
        } else if (this.metadataReaderCache != null) {
            synchronized (this.metadataReaderCache) {
                MetadataReader metadataReader = this.metadataReaderCache.get(resource);
                if (metadataReader == null) {
                    metadataReader = super.getMetadataReader(resource);
                    this.metadataReaderCache.put(resource, metadataReader);
                }
                return metadataReader;
            }
        } else {
            return super.getMetadataReader(resource);
        }
    }

    public void clearCache() {
        if (this.metadataReaderCache instanceof LocalResourceCache) {
            synchronized (this.metadataReaderCache) {
                this.metadataReaderCache.clear();
            }
        } else if (this.metadataReaderCache != null) {
            // Shared resource cache -> reset to local cache.
            setCacheLimit(DEFAULT_CACHE_LIMIT);
        }
    }

    @SuppressWarnings("serial")
    private static class LocalResourceCache extends LinkedHashMap<Resource, MetadataReader> {

        private volatile int cacheLimit;

        public LocalResourceCache(int cacheLimit) {
            super(cacheLimit, 0.75f, true);
            this.cacheLimit = cacheLimit;
        }

        public void setCacheLimit(int cacheLimit) {
            this.cacheLimit = cacheLimit;
        }

        public int getCacheLimit() {
            return this.cacheLimit;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Resource, MetadataReader> eldest) {
            return size() > this.cacheLimit;
        }

    }

}
