package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
public class UriTemplate implements Serializable {

    private final String uriTemplate;

    private final UriComponents uriComponents;

    private final List<String> variableNames;

    private final Pattern matchPattern;

    public UriTemplate(String uriTemplate) {
        Assert.hasText(uriTemplate, "'uriTemplate' must not be null");
        this.uriTemplate = uriTemplate;
        this.uriComponents = UriComponentsBuilder.fromUriString(uriTemplate).build();
        TemplateInfo info = TemplateInfo.parse(uriTemplate);
        this.variableNames = Collections.unmodifiableList(info.getVariableNames());
        this.matchPattern = info.getMatchPattern();
    }

    public List<String> getVariableNames() {
        return this.variableNames;
    }

    public URI expand(Map<String, ?> uriVariables) {
        UriComponents expandedComponents = this.uriComponents.expand(uriVariables);
        UriComponents encodedComponents = expandedComponents.encode();
        return encodedComponents.toUri();
    }

    public URI expand(Object... uriVariableValues) {
        UriComponents expandedComponents = this.uriComponents.expand(uriVariableValues);
        UriComponents encodedComponents = expandedComponents.encode();
        return encodedComponents.toUri();
    }

    public boolean matches(@Nullable String uri) {
        if (uri == null) {
            return false;
        }
        Matcher matcher = this.matchPattern.matcher(uri);
        return matcher.matches();
    }

    public Map<String, String> match(String uri) {
        Assert.notNull(uri, "'uri' must not be null");
        Map<String, String> result = new LinkedHashMap<>(this.variableNames.size());
        Matcher matcher = this.matchPattern.matcher(uri);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String name = this.variableNames.get(i - 1);
                String value = matcher.group(i);
                result.put(name, value);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return this.uriTemplate;
    }

    private static final class TemplateInfo {

        private final List<String> variableNames;

        private final Pattern pattern;

        private TemplateInfo(List<String> vars, Pattern pattern) {
            this.variableNames = vars;
            this.pattern = pattern;
        }

        public List<String> getVariableNames() {
            return this.variableNames;
        }

        public Pattern getMatchPattern() {
            return this.pattern;
        }

        public static TemplateInfo parse(String uriTemplate) {
            int level = 0;
            List<String> variableNames = new ArrayList<>();
            StringBuilder pattern = new StringBuilder();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < uriTemplate.length(); i++) {
                char c = uriTemplate.charAt(i);
                if (c == '{') {
                    level++;
                    if (level == 1) {
                        // start of URI variable
                        pattern.append(quote(builder));
                        builder = new StringBuilder();
                        continue;
                    }
                } else if (c == '}') {
                    level--;
                    if (level == 0) {
                        // end of URI variable
                        String variable = builder.toString();
                        int idx = variable.indexOf(':');
                        if (idx == -1) {
                            pattern.append("([^/]*)");
                            variableNames.add(variable);
                        } else {
                            if (idx + 1 == variable.length()) {
                                throw new IllegalArgumentException("No custom regular expression specified after ':' in \"" + variable + "\"");
                            }
                            String regex = variable.substring(idx + 1, variable.length());
                            pattern.append('(');
                            pattern.append(regex);
                            pattern.append(')');
                            variableNames.add(variable.substring(0, idx));
                        }
                        builder = new StringBuilder();
                        continue;
                    }
                }
                builder.append(c);
            }
            if (builder.length() > 0) {
                pattern.append(quote(builder));
            }
            return new TemplateInfo(variableNames, Pattern.compile(pattern.toString()));
        }

        private static String quote(StringBuilder builder) {
            return (builder.length() > 0 ? Pattern.quote(builder.toString()) : "");
        }

    }

}
