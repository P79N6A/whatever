package org.springframework.ui.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.ui.context.HierarchicalThemeSource;
import org.springframework.ui.context.Theme;
import org.springframework.ui.context.ThemeSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceBundleThemeSource implements HierarchicalThemeSource, BeanClassLoaderAware {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private ThemeSource parentThemeSource;

    private String basenamePrefix = "";

    @Nullable
    private String defaultEncoding;

    @Nullable
    private Boolean fallbackToSystemLocale;

    @Nullable
    private ClassLoader beanClassLoader;

    private final Map<String, Theme> themeCache = new ConcurrentHashMap<>();

    @Override
    public void setParentThemeSource(@Nullable ThemeSource parent) {
        this.parentThemeSource = parent;
        // Update existing Theme objects.
        // Usually there shouldn't be any at the time of this call.
        synchronized (this.themeCache) {
            for (Theme theme : this.themeCache.values()) {
                initParent(theme);
            }
        }
    }

    @Override
    @Nullable
    public ThemeSource getParentThemeSource() {
        return this.parentThemeSource;
    }

    public void setBasenamePrefix(@Nullable String basenamePrefix) {
        this.basenamePrefix = (basenamePrefix != null ? basenamePrefix : "");
    }

    public void setDefaultEncoding(@Nullable String defaultEncoding) {
        this.defaultEncoding = defaultEncoding;
    }

    public void setFallbackToSystemLocale(boolean fallbackToSystemLocale) {
        this.fallbackToSystemLocale = fallbackToSystemLocale;
    }

    @Override
    public void setBeanClassLoader(@Nullable ClassLoader beanClassLoader) {
        this.beanClassLoader = beanClassLoader;
    }

    @Override
    @Nullable
    public Theme getTheme(String themeName) {
        Theme theme = this.themeCache.get(themeName);
        if (theme == null) {
            synchronized (this.themeCache) {
                theme = this.themeCache.get(themeName);
                if (theme == null) {
                    String basename = this.basenamePrefix + themeName;
                    MessageSource messageSource = createMessageSource(basename);
                    theme = new SimpleTheme(themeName, messageSource);
                    initParent(theme);
                    this.themeCache.put(themeName, theme);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Theme created: name '" + themeName + "', basename [" + basename + "]");
                    }
                }
            }
        }
        return theme;
    }

    protected MessageSource createMessageSource(String basename) {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename(basename);
        if (this.defaultEncoding != null) {
            messageSource.setDefaultEncoding(this.defaultEncoding);
        }
        if (this.fallbackToSystemLocale != null) {
            messageSource.setFallbackToSystemLocale(this.fallbackToSystemLocale);
        }
        if (this.beanClassLoader != null) {
            messageSource.setBeanClassLoader(this.beanClassLoader);
        }
        return messageSource;
    }

    protected void initParent(Theme theme) {
        if (theme.getMessageSource() instanceof HierarchicalMessageSource) {
            HierarchicalMessageSource messageSource = (HierarchicalMessageSource) theme.getMessageSource();
            if (getParentThemeSource() != null && messageSource.getParentMessageSource() == null) {
                Theme parentTheme = getParentThemeSource().getTheme(theme.getName());
                if (parentTheme != null) {
                    messageSource.setParentMessageSource(parentTheme.getMessageSource());
                }
            }
        }
    }

}
