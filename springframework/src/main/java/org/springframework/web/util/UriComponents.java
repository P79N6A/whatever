package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

import java.io.Serializable;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public abstract class UriComponents implements Serializable {

    private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)\\}");

    @Nullable
    private final String scheme;

    @Nullable
    private final String fragment;

    protected UriComponents(@Nullable String scheme, @Nullable String fragment) {
        this.scheme = scheme;
        this.fragment = fragment;
    }
    // Component getters

    @Nullable
    public final String getScheme() {
        return this.scheme;
    }

    @Nullable
    public final String getFragment() {
        return this.fragment;
    }

    @Nullable
    public abstract String getSchemeSpecificPart();

    @Nullable
    public abstract String getUserInfo();

    @Nullable
    public abstract String getHost();

    public abstract int getPort();

    @Nullable
    public abstract String getPath();

    public abstract List<String> getPathSegments();

    @Nullable
    public abstract String getQuery();

    public abstract MultiValueMap<String, String> getQueryParams();

    public final UriComponents encode() {
        return encode(StandardCharsets.UTF_8);
    }

    public abstract UriComponents encode(Charset charset);

    public final UriComponents expand(Map<String, ?> uriVariables) {
        Assert.notNull(uriVariables, "'uriVariables' must not be null");
        return expandInternal(new MapTemplateVariables(uriVariables));
    }

    public final UriComponents expand(Object... uriVariableValues) {
        Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");
        return expandInternal(new VarArgsTemplateVariables(uriVariableValues));
    }

    public final UriComponents expand(UriTemplateVariables uriVariables) {
        Assert.notNull(uriVariables, "'uriVariables' must not be null");
        return expandInternal(uriVariables);
    }

    abstract UriComponents expandInternal(UriTemplateVariables uriVariables);

    public abstract UriComponents normalize();

    public abstract String toUriString();

    public abstract URI toUri();

    @Override
    public final String toString() {
        return toUriString();
    }

    protected abstract void copyToUriComponentsBuilder(UriComponentsBuilder builder);
    // Static expansion helpers

    @Nullable
    static String expandUriComponent(@Nullable String source, UriTemplateVariables uriVariables) {
        return expandUriComponent(source, uriVariables, null);
    }

    @Nullable
    static String expandUriComponent(@Nullable String source, UriTemplateVariables uriVariables, @Nullable UnaryOperator<String> encoder) {
        if (source == null) {
            return null;
        }
        if (source.indexOf('{') == -1) {
            return source;
        }
        if (source.indexOf(':') != -1) {
            source = sanitizeSource(source);
        }
        Matcher matcher = NAMES_PATTERN.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String match = matcher.group(1);
            String varName = getVariableName(match);
            Object varValue = uriVariables.getValue(varName);
            if (UriTemplateVariables.SKIP_VALUE.equals(varValue)) {
                continue;
            }
            String formatted = getVariableValueAsString(varValue);
            formatted = encoder != null ? encoder.apply(formatted) : Matcher.quoteReplacement(formatted);
            matcher.appendReplacement(sb, formatted);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String sanitizeSource(String source) {
        int level = 0;
        StringBuilder sb = new StringBuilder();
        for (char c : source.toCharArray()) {
            if (c == '{') {
                level++;
            }
            if (c == '}') {
                level--;
            }
            if (level > 1 || (level == 1 && c == '}')) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String getVariableName(String match) {
        int colonIdx = match.indexOf(':');
        return (colonIdx != -1 ? match.substring(0, colonIdx) : match);
    }

    private static String getVariableValueAsString(@Nullable Object variableValue) {
        return (variableValue != null ? variableValue.toString() : "");
    }

    public interface UriTemplateVariables {

        Object SKIP_VALUE = UriTemplateVariables.class;

        @Nullable
        Object getValue(@Nullable String name);

    }

    private static class MapTemplateVariables implements UriTemplateVariables {

        private final Map<String, ?> uriVariables;

        public MapTemplateVariables(Map<String, ?> uriVariables) {
            this.uriVariables = uriVariables;
        }

        @Override
        @Nullable
        public Object getValue(@Nullable String name) {
            if (!this.uriVariables.containsKey(name)) {
                throw new IllegalArgumentException("Map has no value for '" + name + "'");
            }
            return this.uriVariables.get(name);
        }

    }

    private static class VarArgsTemplateVariables implements UriTemplateVariables {

        private final Iterator<Object> valueIterator;

        public VarArgsTemplateVariables(Object... uriVariableValues) {
            this.valueIterator = Arrays.asList(uriVariableValues).iterator();
        }

        @Override
        @Nullable
        public Object getValue(@Nullable String name) {
            if (!this.valueIterator.hasNext()) {
                throw new IllegalArgumentException("Not enough variable values available to expand '" + name + "'");
            }
            return this.valueIterator.next();
        }

    }

}
