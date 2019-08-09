package org.springframework.context.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractResourceBasedMessageSource extends AbstractMessageSource {

    private final Set<String> basenameSet = new LinkedHashSet<>(4);

    @Nullable
    private String defaultEncoding;

    private boolean fallbackToSystemLocale = true;

    private long cacheMillis = -1;

    public void setBasename(String basename) {
        setBasenames(basename);
    }

    public void setBasenames(String... basenames) {
        this.basenameSet.clear();
        addBasenames(basenames);
    }

    public void addBasenames(String... basenames) {
        if (!ObjectUtils.isEmpty(basenames)) {
            for (String basename : basenames) {
                Assert.hasText(basename, "Basename must not be empty");
                this.basenameSet.add(basename.trim());
            }
        }
    }

    public Set<String> getBasenameSet() {
        return this.basenameSet;
    }

    public void setDefaultEncoding(@Nullable String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    @Nullable
    protected String getDefaultEncoding() {
        return this.defaultEncoding;
    }

    public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
        this.fallbackToSystemLocale = fallbackToSystemLocale;
    }

    protected boolean isFallbackToSystemLocale() {
        return this.fallbackToSystemLocale;
    }

    public void setCacheSeconds(int cacheSeconds) {
        this.cacheMillis = (cacheSeconds * 1000);
    }

    public void setCacheMillis(long cacheMillis) {
        this.cacheMillis = cacheMillis;
    }

    protected long getCacheMillis() {
        return this.cacheMillis;
    }

}
