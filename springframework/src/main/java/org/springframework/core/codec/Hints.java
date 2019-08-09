package org.springframework.core.codec;

import org.apache.commons.logging.Log;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Hints {

    public static final String LOG_PREFIX_HINT = Log.class.getName() + ".PREFIX";

    public static final String SUPPRESS_LOGGING_HINT = Log.class.getName() + ".SUPPRESS_LOGGING";

    public static Map<String, Object> from(String hintName, Object value) {
        return Collections.singletonMap(hintName, value);
    }

    public static Map<String, Object> none() {
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getRequiredHint(@Nullable Map<String, Object> hints, String hintName) {
        if (hints == null) {
            throw new IllegalArgumentException("No hints map for required hint '" + hintName + "'");
        }
        T hint = (T) hints.get(hintName);
        if (hint == null) {
            throw new IllegalArgumentException("Hints map must contain the hint '" + hintName + "'");
        }
        return hint;
    }

    public static String getLogPrefix(@Nullable Map<String, Object> hints) {
        return (hints != null ? (String) hints.getOrDefault(LOG_PREFIX_HINT, "") : "");
    }

    public static boolean isLoggingSuppressed(@Nullable Map<String, Object> hints) {
        return (hints != null && (boolean) hints.getOrDefault(SUPPRESS_LOGGING_HINT, false));
    }

    public static Map<String, Object> merge(Map<String, Object> hints1, Map<String, Object> hints2) {
        if (hints1.isEmpty() && hints2.isEmpty()) {
            return Collections.emptyMap();
        } else if (hints2.isEmpty()) {
            return hints1;
        } else if (hints1.isEmpty()) {
            return hints2;
        } else {
            Map<String, Object> result = new HashMap<>(hints1.size() + hints2.size());
            result.putAll(hints1);
            result.putAll(hints2);
            return result;
        }
    }

    public static Map<String, Object> merge(Map<String, Object> hints, String hintName, Object hintValue) {
        if (hints.isEmpty()) {
            return Collections.singletonMap(hintName, hintValue);
        } else {
            Map<String, Object> result = new HashMap<>(hints.size() + 1);
            result.putAll(hints);
            result.put(hintName, hintValue);
            return result;
        }
    }

}
