package org.springframework.context.index;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class CandidateComponentsIndex {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher(".");

    private final MultiValueMap<String, Entry> index;

    CandidateComponentsIndex(List<Properties> content) {
        this.index = parseIndex(content);
    }

    public Set<String> getCandidateTypes(String basePackage, String stereotype) {
        List<Entry> candidates = this.index.get(stereotype);
        if (candidates != null) {
            return candidates.parallelStream().filter(t -> t.match(basePackage)).map(t -> t.type).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private static MultiValueMap<String, Entry> parseIndex(List<Properties> content) {
        MultiValueMap<String, Entry> index = new LinkedMultiValueMap<>();
        for (Properties entry : content) {
            entry.forEach((type, values) -> {
                String[] stereotypes = ((String) values).split(",");
                for (String stereotype : stereotypes) {
                    index.add(stereotype, new Entry((String) type));
                }
            });
        }
        return index;
    }

    private static class Entry {
        private final String type;

        private final String packageName;

        Entry(String type) {
            this.type = type;
            this.packageName = ClassUtils.getPackageName(type);
        }

        public boolean match(String basePackage) {
            if (pathMatcher.isPattern(basePackage)) {
                return pathMatcher.match(basePackage, this.packageName);
            } else {
                return this.type.startsWith(basePackage);
            }
        }

    }

}
