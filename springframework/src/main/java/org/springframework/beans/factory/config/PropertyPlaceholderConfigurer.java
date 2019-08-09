package org.springframework.beans.factory.config;

import org.springframework.beans.BeansException;
import org.springframework.core.Constants;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;

import java.util.Properties;

@Deprecated
public class PropertyPlaceholderConfigurer extends PlaceholderConfigurerSupport {

    public static final int SYSTEM_PROPERTIES_MODE_NEVER = 0;

    public static final int SYSTEM_PROPERTIES_MODE_FALLBACK = 1;

    public static final int SYSTEM_PROPERTIES_MODE_OVERRIDE = 2;

    private static final Constants constants = new Constants(PropertyPlaceholderConfigurer.class);

    private int systemPropertiesMode = SYSTEM_PROPERTIES_MODE_FALLBACK;

    private boolean searchSystemEnvironment = !SpringProperties.getFlag(AbstractEnvironment.IGNORE_GETENV_PROPERTY_NAME);

    public void setSystemPropertiesModeName(String constantName) throws IllegalArgumentException {
        this.systemPropertiesMode = constants.asNumber(constantName).intValue();
    }

    public void setSystemPropertiesMode(int systemPropertiesMode) {
        this.systemPropertiesMode = systemPropertiesMode;
    }

    public void setSearchSystemEnvironment(boolean searchSystemEnvironment) {
        this.searchSystemEnvironment = searchSystemEnvironment;
    }

    @Nullable
    protected String resolvePlaceholder(String placeholder, Properties props, int systemPropertiesMode) {
        String propVal = null;
        if (systemPropertiesMode == SYSTEM_PROPERTIES_MODE_OVERRIDE) {
            propVal = resolveSystemProperty(placeholder);
        }
        if (propVal == null) {
            propVal = resolvePlaceholder(placeholder, props);
        }
        if (propVal == null && systemPropertiesMode == SYSTEM_PROPERTIES_MODE_FALLBACK) {
            propVal = resolveSystemProperty(placeholder);
        }
        return propVal;
    }

    @Nullable
    protected String resolvePlaceholder(String placeholder, Properties props) {
        return props.getProperty(placeholder);
    }

    @Nullable
    protected String resolveSystemProperty(String key) {
        try {
            String value = System.getProperty(key);
            if (value == null && this.searchSystemEnvironment) {
                value = System.getenv(key);
            }
            return value;
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Could not access system property '" + key + "': " + ex);
            }
            return null;
        }
    }

    @Override
    protected void processProperties(ConfigurableListableBeanFactory beanFactoryToProcess, Properties props) throws BeansException {
        StringValueResolver valueResolver = new PlaceholderResolvingStringValueResolver(props);
        doProcessProperties(beanFactoryToProcess, valueResolver);
    }

    private class PlaceholderResolvingStringValueResolver implements StringValueResolver {

        private final PropertyPlaceholderHelper helper;

        private final PlaceholderResolver resolver;

        public PlaceholderResolvingStringValueResolver(Properties props) {
            this.helper = new PropertyPlaceholderHelper(placeholderPrefix, placeholderSuffix, valueSeparator, ignoreUnresolvablePlaceholders);
            this.resolver = new PropertyPlaceholderConfigurerResolver(props);
        }

        @Override
        @Nullable
        public String resolveStringValue(String strVal) throws BeansException {
            String resolved = this.helper.replacePlaceholders(strVal, this.resolver);
            if (trimValues) {
                resolved = resolved.trim();
            }
            return (resolved.equals(nullValue) ? null : resolved);
        }

    }

    private final class PropertyPlaceholderConfigurerResolver implements PlaceholderResolver {

        private final Properties props;

        private PropertyPlaceholderConfigurerResolver(Properties props) {
            this.props = props;
        }

        @Override
        @Nullable
        public String resolvePlaceholder(String placeholderName) {
            return PropertyPlaceholderConfigurer.this.resolvePlaceholder(placeholderName, this.props, systemPropertiesMode);
        }

    }

}
