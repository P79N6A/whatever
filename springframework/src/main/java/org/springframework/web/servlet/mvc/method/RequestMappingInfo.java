package org.springframework.web.servlet.mvc.method;

import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.*;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

public final class RequestMappingInfo implements RequestCondition<RequestMappingInfo> {

    @Nullable
    private final String name;

    private final PatternsRequestCondition patternsCondition;

    private final RequestMethodsRequestCondition methodsCondition;

    private final ParamsRequestCondition paramsCondition;

    private final HeadersRequestCondition headersCondition;

    private final ConsumesRequestCondition consumesCondition;

    private final ProducesRequestCondition producesCondition;

    private final RequestConditionHolder customConditionHolder;

    public RequestMappingInfo(@Nullable String name, @Nullable PatternsRequestCondition patterns, @Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params, @Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes, @Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {
        this.name = (StringUtils.hasText(name) ? name : null);
        this.patternsCondition = (patterns != null ? patterns : new PatternsRequestCondition());
        this.methodsCondition = (methods != null ? methods : new RequestMethodsRequestCondition());
        this.paramsCondition = (params != null ? params : new ParamsRequestCondition());
        this.headersCondition = (headers != null ? headers : new HeadersRequestCondition());
        this.consumesCondition = (consumes != null ? consumes : new ConsumesRequestCondition());
        this.producesCondition = (produces != null ? produces : new ProducesRequestCondition());
        this.customConditionHolder = new RequestConditionHolder(custom);
    }

    public RequestMappingInfo(@Nullable PatternsRequestCondition patterns, @Nullable RequestMethodsRequestCondition methods, @Nullable ParamsRequestCondition params, @Nullable HeadersRequestCondition headers, @Nullable ConsumesRequestCondition consumes, @Nullable ProducesRequestCondition produces, @Nullable RequestCondition<?> custom) {
        this(null, patterns, methods, params, headers, consumes, produces, custom);
    }

    public RequestMappingInfo(RequestMappingInfo info, @Nullable RequestCondition<?> customRequestCondition) {
        this(info.name, info.patternsCondition, info.methodsCondition, info.paramsCondition, info.headersCondition, info.consumesCondition, info.producesCondition, customRequestCondition);
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public PatternsRequestCondition getPatternsCondition() {
        return this.patternsCondition;
    }

    public RequestMethodsRequestCondition getMethodsCondition() {
        return this.methodsCondition;
    }

    public ParamsRequestCondition getParamsCondition() {
        return this.paramsCondition;
    }

    public HeadersRequestCondition getHeadersCondition() {
        return this.headersCondition;
    }

    public ConsumesRequestCondition getConsumesCondition() {
        return this.consumesCondition;
    }

    public ProducesRequestCondition getProducesCondition() {
        return this.producesCondition;
    }

    @Nullable
    public RequestCondition<?> getCustomCondition() {
        return this.customConditionHolder.getCondition();
    }

    @Override
    public RequestMappingInfo combine(RequestMappingInfo other) {
        String name = combineNames(other);
        PatternsRequestCondition patterns = this.patternsCondition.combine(other.patternsCondition);
        RequestMethodsRequestCondition methods = this.methodsCondition.combine(other.methodsCondition);
        ParamsRequestCondition params = this.paramsCondition.combine(other.paramsCondition);
        HeadersRequestCondition headers = this.headersCondition.combine(other.headersCondition);
        ConsumesRequestCondition consumes = this.consumesCondition.combine(other.consumesCondition);
        ProducesRequestCondition produces = this.producesCondition.combine(other.producesCondition);
        RequestConditionHolder custom = this.customConditionHolder.combine(other.customConditionHolder);
        return new RequestMappingInfo(name, patterns, methods, params, headers, consumes, produces, custom.getCondition());
    }

    @Nullable
    private String combineNames(RequestMappingInfo other) {
        if (this.name != null && other.name != null) {
            String separator = RequestMappingInfoHandlerMethodMappingNamingStrategy.SEPARATOR;
            return this.name + separator + other.name;
        } else if (this.name != null) {
            return this.name;
        } else {
            return other.name;
        }
    }

    @Override
    @Nullable
    public RequestMappingInfo getMatchingCondition(HttpServletRequest request) {
        RequestMethodsRequestCondition methods = this.methodsCondition.getMatchingCondition(request);
        if (methods == null) {
            return null;
        }
        ParamsRequestCondition params = this.paramsCondition.getMatchingCondition(request);
        if (params == null) {
            return null;
        }
        HeadersRequestCondition headers = this.headersCondition.getMatchingCondition(request);
        if (headers == null) {
            return null;
        }
        ConsumesRequestCondition consumes = this.consumesCondition.getMatchingCondition(request);
        if (consumes == null) {
            return null;
        }
        ProducesRequestCondition produces = this.producesCondition.getMatchingCondition(request);
        if (produces == null) {
            return null;
        }
        PatternsRequestCondition patterns = this.patternsCondition.getMatchingCondition(request);
        if (patterns == null) {
            return null;
        }
        RequestConditionHolder custom = this.customConditionHolder.getMatchingCondition(request);
        if (custom == null) {
            return null;
        }
        return new RequestMappingInfo(this.name, patterns, methods, params, headers, consumes, produces, custom.getCondition());
    }

    @Override
    public int compareTo(RequestMappingInfo other, HttpServletRequest request) {
        int result;
        // Automatic vs explicit HTTP HEAD mapping
        if (HttpMethod.HEAD.matches(request.getMethod())) {
            result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
            if (result != 0) {
                return result;
            }
        }
        result = this.patternsCondition.compareTo(other.getPatternsCondition(), request);
        if (result != 0) {
            return result;
        }
        result = this.paramsCondition.compareTo(other.getParamsCondition(), request);
        if (result != 0) {
            return result;
        }
        result = this.headersCondition.compareTo(other.getHeadersCondition(), request);
        if (result != 0) {
            return result;
        }
        result = this.consumesCondition.compareTo(other.getConsumesCondition(), request);
        if (result != 0) {
            return result;
        }
        result = this.producesCondition.compareTo(other.getProducesCondition(), request);
        if (result != 0) {
            return result;
        }
        // Implicit (no method) vs explicit HTTP method mappings
        result = this.methodsCondition.compareTo(other.getMethodsCondition(), request);
        if (result != 0) {
            return result;
        }
        result = this.customConditionHolder.compareTo(other.customConditionHolder, request);
        if (result != 0) {
            return result;
        }
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RequestMappingInfo)) {
            return false;
        }
        RequestMappingInfo otherInfo = (RequestMappingInfo) other;
        return (this.patternsCondition.equals(otherInfo.patternsCondition) && this.methodsCondition.equals(otherInfo.methodsCondition) && this.paramsCondition.equals(otherInfo.paramsCondition) && this.headersCondition.equals(otherInfo.headersCondition) && this.consumesCondition.equals(otherInfo.consumesCondition) && this.producesCondition.equals(otherInfo.producesCondition) && this.customConditionHolder.equals(otherInfo.customConditionHolder));
    }

    @Override
    public int hashCode() {
        return (this.patternsCondition.hashCode() * 31 +  // primary differentiation
                this.methodsCondition.hashCode() + this.paramsCondition.hashCode() + this.headersCondition.hashCode() + this.consumesCondition.hashCode() + this.producesCondition.hashCode() + this.customConditionHolder.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{");
        if (!this.methodsCondition.isEmpty()) {
            Set<RequestMethod> httpMethods = this.methodsCondition.getMethods();
            builder.append(httpMethods.size() == 1 ? httpMethods.iterator().next() : httpMethods);
        }
        if (!this.patternsCondition.isEmpty()) {
            Set<String> patterns = this.patternsCondition.getPatterns();
            builder.append(" ").append(patterns.size() == 1 ? patterns.iterator().next() : patterns);
        }
        if (!this.paramsCondition.isEmpty()) {
            builder.append(", params ").append(this.paramsCondition);
        }
        if (!this.headersCondition.isEmpty()) {
            builder.append(", headers ").append(this.headersCondition);
        }
        if (!this.consumesCondition.isEmpty()) {
            builder.append(", consumes ").append(this.consumesCondition);
        }
        if (!this.producesCondition.isEmpty()) {
            builder.append(", produces ").append(this.producesCondition);
        }
        if (!this.customConditionHolder.isEmpty()) {
            builder.append(", and ").append(this.customConditionHolder);
        }
        builder.append('}');
        return builder.toString();
    }

    public static Builder paths(String... paths) {
        return new DefaultBuilder(paths);
    }

    public interface Builder {

        Builder paths(String... paths);

        Builder methods(RequestMethod... methods);

        Builder params(String... params);

        Builder headers(String... headers);

        Builder consumes(String... consumes);

        Builder produces(String... produces);

        Builder mappingName(String name);

        Builder customCondition(RequestCondition<?> condition);

        Builder options(BuilderConfiguration options);

        RequestMappingInfo build();

    }

    private static class DefaultBuilder implements Builder {

        private String[] paths = new String[0];

        private RequestMethod[] methods = new RequestMethod[0];

        private String[] params = new String[0];

        private String[] headers = new String[0];

        private String[] consumes = new String[0];

        private String[] produces = new String[0];

        @Nullable
        private String mappingName;

        @Nullable
        private RequestCondition<?> customCondition;

        private BuilderConfiguration options = new BuilderConfiguration();

        public DefaultBuilder(String... paths) {
            this.paths = paths;
        }

        @Override
        public Builder paths(String... paths) {
            this.paths = paths;
            return this;
        }

        @Override
        public DefaultBuilder methods(RequestMethod... methods) {
            this.methods = methods;
            return this;
        }

        @Override
        public DefaultBuilder params(String... params) {
            this.params = params;
            return this;
        }

        @Override
        public DefaultBuilder headers(String... headers) {
            this.headers = headers;
            return this;
        }

        @Override
        public DefaultBuilder consumes(String... consumes) {
            this.consumes = consumes;
            return this;
        }

        @Override
        public DefaultBuilder produces(String... produces) {
            this.produces = produces;
            return this;
        }

        @Override
        public DefaultBuilder mappingName(String name) {
            this.mappingName = name;
            return this;
        }

        @Override
        public DefaultBuilder customCondition(RequestCondition<?> condition) {
            this.customCondition = condition;
            return this;
        }

        @Override
        public Builder options(BuilderConfiguration options) {
            this.options = options;
            return this;
        }

        @Override
        public RequestMappingInfo build() {
            ContentNegotiationManager manager = this.options.getContentNegotiationManager();
            PatternsRequestCondition patternsCondition = new PatternsRequestCondition(this.paths, this.options.getUrlPathHelper(), this.options.getPathMatcher(), this.options.useSuffixPatternMatch(), this.options.useTrailingSlashMatch(), this.options.getFileExtensions());
            return new RequestMappingInfo(this.mappingName, patternsCondition, new RequestMethodsRequestCondition(this.methods), new ParamsRequestCondition(this.params), new HeadersRequestCondition(this.headers), new ConsumesRequestCondition(this.consumes, this.headers), new ProducesRequestCondition(this.produces, this.headers, manager), this.customCondition);
        }

    }

    public static class BuilderConfiguration {

        @Nullable
        private UrlPathHelper urlPathHelper;

        @Nullable
        private PathMatcher pathMatcher;

        private boolean trailingSlashMatch = true;

        private boolean suffixPatternMatch = true;

        private boolean registeredSuffixPatternMatch = false;

        @Nullable
        private ContentNegotiationManager contentNegotiationManager;

        public void setUrlPathHelper(@Nullable UrlPathHelper urlPathHelper) {
            this.urlPathHelper = urlPathHelper;
        }

        @Nullable
        public UrlPathHelper getUrlPathHelper() {
            return this.urlPathHelper;
        }

        public void setPathMatcher(@Nullable PathMatcher pathMatcher) {
            this.pathMatcher = pathMatcher;
        }

        @Nullable
        public PathMatcher getPathMatcher() {
            return this.pathMatcher;
        }

        public void setTrailingSlashMatch(boolean trailingSlashMatch) {
            this.trailingSlashMatch = trailingSlashMatch;
        }

        public boolean useTrailingSlashMatch() {
            return this.trailingSlashMatch;
        }

        public void setSuffixPatternMatch(boolean suffixPatternMatch) {
            this.suffixPatternMatch = suffixPatternMatch;
        }

        public boolean useSuffixPatternMatch() {
            return this.suffixPatternMatch;
        }

        public void setRegisteredSuffixPatternMatch(boolean registeredSuffixPatternMatch) {
            this.registeredSuffixPatternMatch = registeredSuffixPatternMatch;
            this.suffixPatternMatch = (registeredSuffixPatternMatch || this.suffixPatternMatch);
        }

        public boolean useRegisteredSuffixPatternMatch() {
            return this.registeredSuffixPatternMatch;
        }

        @Nullable
        public List<String> getFileExtensions() {
            if (useRegisteredSuffixPatternMatch() && this.contentNegotiationManager != null) {
                return this.contentNegotiationManager.getAllFileExtensions();
            }
            return null;
        }

        public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
            this.contentNegotiationManager = contentNegotiationManager;
        }

        @Nullable
        public ContentNegotiationManager getContentNegotiationManager() {
            return this.contentNegotiationManager;
        }

    }

}
