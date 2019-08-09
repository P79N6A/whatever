package org.springframework.beans.factory.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.CollectionFactory;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

public abstract class YamlProcessor {

    private final Log logger = LogFactory.getLog(getClass());

    private ResolutionMethod resolutionMethod = ResolutionMethod.OVERRIDE;

    private Resource[] resources = new Resource[0];

    private List<DocumentMatcher> documentMatchers = Collections.emptyList();

    private boolean matchDefault = true;

    public void setDocumentMatchers(DocumentMatcher... matchers) {
        this.documentMatchers = Arrays.asList(matchers);
    }

    public void setMatchDefault(boolean matchDefault) {
        this.matchDefault = matchDefault;
    }

    public void setResolutionMethod(ResolutionMethod resolutionMethod) {
        Assert.notNull(resolutionMethod, "ResolutionMethod must not be null");
        this.resolutionMethod = resolutionMethod;
    }

    public void setResources(Resource... resources) {
        this.resources = resources;
    }

    protected void process(MatchCallback callback) {
        Yaml yaml = createYaml();
        for (Resource resource : this.resources) {
            boolean found = process(callback, yaml, resource);
            if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND && found) {
                return;
            }
        }
    }

    protected Yaml createYaml() {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        return new Yaml(options);
    }

    private boolean process(MatchCallback callback, Yaml yaml, Resource resource) {
        int count = 0;
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Loading from YAML: " + resource);
            }
            try (Reader reader = new UnicodeReader(resource.getInputStream())) {
                for (Object object : yaml.loadAll(reader)) {
                    if (object != null && process(asMap(object), callback)) {
                        count++;
                        if (this.resolutionMethod == ResolutionMethod.FIRST_FOUND) {
                            break;
                        }
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Loaded " + count + " document" + (count > 1 ? "s" : "") + " from YAML resource: " + resource);
                }
            }
        } catch (IOException ex) {
            handleProcessError(resource, ex);
        }
        return (count > 0);
    }

    private void handleProcessError(Resource resource, IOException ex) {
        if (this.resolutionMethod != ResolutionMethod.FIRST_FOUND && this.resolutionMethod != ResolutionMethod.OVERRIDE_AND_IGNORE) {
            throw new IllegalStateException(ex);
        }
        if (logger.isWarnEnabled()) {
            logger.warn("Could not load map from " + resource + ": " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object object) {
        // YAML can have numbers as keys
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            // A document can be a text literal
            result.put("document", object);
            return result;
        }
        Map<Object, Object> map = (Map<Object, Object>) object;
        map.forEach((key, value) -> {
            if (value instanceof Map) {
                value = asMap(value);
            }
            if (key instanceof CharSequence) {
                result.put(key.toString(), value);
            } else {
                // It has to be a map key in this case
                result.put("[" + key.toString() + "]", value);
            }
        });
        return result;
    }

    private boolean process(Map<String, Object> map, MatchCallback callback) {
        Properties properties = CollectionFactory.createStringAdaptingProperties();
        properties.putAll(getFlattenedMap(map));
        if (this.documentMatchers.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Merging document (no matchers set): " + map);
            }
            callback.process(properties, map);
            return true;
        }
        MatchStatus result = MatchStatus.ABSTAIN;
        for (DocumentMatcher matcher : this.documentMatchers) {
            MatchStatus match = matcher.matches(properties);
            result = MatchStatus.getMostSpecific(match, result);
            if (match == MatchStatus.FOUND) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Matched document with document matcher: " + properties);
                }
                callback.process(properties, map);
                return true;
            }
        }
        if (result == MatchStatus.ABSTAIN && this.matchDefault) {
            if (logger.isDebugEnabled()) {
                logger.debug("Matched document with default matcher: " + map);
            }
            callback.process(properties, map);
            return true;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Unmatched document: " + map);
        }
        return false;
    }

    protected final Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, @Nullable String path) {
        source.forEach((key, value) -> {
            if (StringUtils.hasText(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                // Need a compound key
                @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            } else if (value instanceof Collection) {
                // Need a compound key
                @SuppressWarnings("unchecked") Collection<Object> collection = (Collection<Object>) value;
                if (collection.isEmpty()) {
                    result.put(key, "");
                } else {
                    int count = 0;
                    for (Object object : collection) {
                        buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
                    }
                }
            } else {
                result.put(key, (value != null ? value : ""));
            }
        });
    }

    public interface MatchCallback {

        void process(Properties properties, Map<String, Object> map);

    }

    public interface DocumentMatcher {

        MatchStatus matches(Properties properties);

    }

    public enum MatchStatus {

        FOUND,

        NOT_FOUND,

        ABSTAIN;

        public static MatchStatus getMostSpecific(MatchStatus a, MatchStatus b) {
            return (a.ordinal() < b.ordinal() ? a : b);
        }
    }

    public enum ResolutionMethod {

        OVERRIDE,

        OVERRIDE_AND_IGNORE,

        FIRST_FOUND
    }

}
