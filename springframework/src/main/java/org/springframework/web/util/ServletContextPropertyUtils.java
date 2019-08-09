package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

import javax.servlet.ServletContext;

public abstract class ServletContextPropertyUtils {

    private static final PropertyPlaceholderHelper strictHelper = new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX, SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, false);

    private static final PropertyPlaceholderHelper nonStrictHelper = new PropertyPlaceholderHelper(SystemPropertyUtils.PLACEHOLDER_PREFIX, SystemPropertyUtils.PLACEHOLDER_SUFFIX, SystemPropertyUtils.VALUE_SEPARATOR, true);

    public static String resolvePlaceholders(String text, ServletContext servletContext) {
        return resolvePlaceholders(text, servletContext, false);
    }

    public static String resolvePlaceholders(String text, ServletContext servletContext, boolean ignoreUnresolvablePlaceholders) {
        PropertyPlaceholderHelper helper = (ignoreUnresolvablePlaceholders ? nonStrictHelper : strictHelper);
        return helper.replacePlaceholders(text, new ServletContextPlaceholderResolver(text, servletContext));
    }

    private static class ServletContextPlaceholderResolver implements PropertyPlaceholderHelper.PlaceholderResolver {

        private final String text;

        private final ServletContext servletContext;

        public ServletContextPlaceholderResolver(String text, ServletContext servletContext) {
            this.text = text;
            this.servletContext = servletContext;
        }

        @Override
        @Nullable
        public String resolvePlaceholder(String placeholderName) {
            try {
                String propVal = this.servletContext.getInitParameter(placeholderName);
                if (propVal == null) {
                    // Fall back to system properties.
                    propVal = System.getProperty(placeholderName);
                    if (propVal == null) {
                        // Fall back to searching the system environment.
                        propVal = System.getenv(placeholderName);
                    }
                }
                return propVal;
            } catch (Throwable ex) {
                System.err.println("Could not resolve placeholder '" + placeholderName + "' in [" + this.text + "] as ServletContext init-parameter or system property: " + ex);
                return null;
            }
        }

    }

}
