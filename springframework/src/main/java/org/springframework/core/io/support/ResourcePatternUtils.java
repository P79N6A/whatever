package org.springframework.core.io.support;

import org.springframework.core.io.ResourceLoader;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

public abstract class ResourcePatternUtils {

    public static boolean isUrl(@Nullable String resourceLocation) {
        return (resourceLocation != null && (resourceLocation.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX) || ResourceUtils.isUrl(resourceLocation)));
    }

    public static ResourcePatternResolver getResourcePatternResolver(@Nullable ResourceLoader resourceLoader) {
        if (resourceLoader instanceof ResourcePatternResolver) {
            return (ResourcePatternResolver) resourceLoader;
        } else if (resourceLoader != null) {
            return new PathMatchingResourcePatternResolver(resourceLoader);
        } else {
            return new PathMatchingResourcePatternResolver();
        }
    }

}
