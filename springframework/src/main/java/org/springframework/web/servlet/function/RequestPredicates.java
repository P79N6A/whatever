package org.springframework.web.servlet.function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class RequestPredicates {

    private static final Log logger = LogFactory.getLog(RequestPredicates.class);

    private static final PathPatternParser DEFAULT_PATTERN_PARSER = new PathPatternParser();

    public static RequestPredicate all() {
        return request -> true;
    }

    public static RequestPredicate method(HttpMethod httpMethod) {
        return new HttpMethodPredicate(httpMethod);
    }

    public static RequestPredicate methods(HttpMethod... httpMethods) {
        return new HttpMethodPredicate(httpMethods);
    }

    public static RequestPredicate path(String pattern) {
        Assert.notNull(pattern, "'pattern' must not be null");
        return pathPredicates(DEFAULT_PATTERN_PARSER).apply(pattern);
    }

    public static Function<String, RequestPredicate> pathPredicates(PathPatternParser patternParser) {
        Assert.notNull(patternParser, "PathPatternParser must not be null");
        return pattern -> new PathPatternPredicate(patternParser.parse(pattern));
    }

    public static RequestPredicate headers(Predicate<ServerRequest.Headers> headersPredicate) {
        return new HeadersPredicate(headersPredicate);
    }

    public static RequestPredicate contentType(MediaType... mediaTypes) {
        Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
        return new ContentTypePredicate(mediaTypes);
    }

    public static RequestPredicate accept(MediaType... mediaTypes) {
        Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
        return new AcceptPredicate(mediaTypes);
    }

    public static RequestPredicate GET(String pattern) {
        return method(HttpMethod.GET).and(path(pattern));
    }

    public static RequestPredicate HEAD(String pattern) {
        return method(HttpMethod.HEAD).and(path(pattern));
    }

    public static RequestPredicate POST(String pattern) {
        return method(HttpMethod.POST).and(path(pattern));
    }

    public static RequestPredicate PUT(String pattern) {
        return method(HttpMethod.PUT).and(path(pattern));
    }

    public static RequestPredicate PATCH(String pattern) {
        return method(HttpMethod.PATCH).and(path(pattern));
    }

    public static RequestPredicate DELETE(String pattern) {
        return method(HttpMethod.DELETE).and(path(pattern));
    }

    public static RequestPredicate OPTIONS(String pattern) {
        return method(HttpMethod.OPTIONS).and(path(pattern));
    }

    public static RequestPredicate pathExtension(String extension) {
        Assert.notNull(extension, "'extension' must not be null");
        return new PathExtensionPredicate(extension);
    }

    public static RequestPredicate pathExtension(Predicate<String> extensionPredicate) {
        return new PathExtensionPredicate(extensionPredicate);
    }

    public static RequestPredicate param(String name, String value) {
        return new ParamPredicate(name, value);
    }

    public static RequestPredicate param(String name, Predicate<String> predicate) {
        return new ParamPredicate(name, predicate);
    }

    private static void traceMatch(String prefix, Object desired, @Nullable Object actual, boolean match) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%s \"%s\" %s against value \"%s\"", prefix, desired, match ? "matches" : "does not match", actual));
        }
    }

    private static void restoreAttributes(ServerRequest request, Map<String, Object> attributes) {
        request.attributes().clear();
        request.attributes().putAll(attributes);
    }

    private static Map<String, String> mergePathVariables(Map<String, String> oldVariables, Map<String, String> newVariables) {
        if (!newVariables.isEmpty()) {
            Map<String, String> mergedVariables = new LinkedHashMap<>(oldVariables);
            mergedVariables.putAll(newVariables);
            return mergedVariables;
        } else {
            return oldVariables;
        }
    }

    private static PathPattern mergePatterns(@Nullable PathPattern oldPattern, PathPattern newPattern) {
        if (oldPattern != null) {
            return oldPattern.combine(newPattern);
        } else {
            return newPattern;
        }

    }

    public interface Visitor {

        void method(Set<HttpMethod> methods);

        void path(String pattern);

        void pathExtension(String extension);

        void header(String name, String value);

        void param(String name, String value);

        void startAnd();

        void and();

        void endAnd();

        void startOr();

        void or();

        void endOr();

        void startNegate();

        void endNegate();

        void unknown(RequestPredicate predicate);

    }

    private static class HttpMethodPredicate implements RequestPredicate {

        private final Set<HttpMethod> httpMethods;

        public HttpMethodPredicate(HttpMethod httpMethod) {
            Assert.notNull(httpMethod, "HttpMethod must not be null");
            this.httpMethods = EnumSet.of(httpMethod);
        }

        public HttpMethodPredicate(HttpMethod... httpMethods) {
            Assert.notEmpty(httpMethods, "HttpMethods must not be empty");
            this.httpMethods = EnumSet.copyOf(Arrays.asList(httpMethods));
        }

        @Override
        public boolean test(ServerRequest request) {
            boolean match = this.httpMethods.contains(request.method());
            traceMatch("Method", this.httpMethods, request.method(), match);
            return match;
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.method(Collections.unmodifiableSet(this.httpMethods));
        }

        @Override
        public String toString() {
            if (this.httpMethods.size() == 1) {
                return this.httpMethods.iterator().next().toString();
            } else {
                return this.httpMethods.toString();
            }
        }

    }

    private static class PathPatternPredicate implements RequestPredicate {

        private final PathPattern pattern;

        public PathPatternPredicate(PathPattern pattern) {
            Assert.notNull(pattern, "'pattern' must not be null");
            this.pattern = pattern;
        }

        @Override
        public boolean test(ServerRequest request) {
            PathContainer pathContainer = request.pathContainer();
            PathPattern.PathMatchInfo info = this.pattern.matchAndExtract(pathContainer);
            traceMatch("Pattern", this.pattern.getPatternString(), request.path(), info != null);
            if (info != null) {
                mergeAttributes(request, info.getUriVariables(), this.pattern);
                return true;
            } else {
                return false;
            }
        }

        private static void mergeAttributes(ServerRequest request, Map<String, String> variables, PathPattern pattern) {
            Map<String, String> pathVariables = mergePathVariables(request.pathVariables(), variables);
            request.attributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.unmodifiableMap(pathVariables));
            pattern = mergePatterns((PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE), pattern);
            request.attributes().put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
        }

        @Override
        public Optional<ServerRequest> nest(ServerRequest request) {
            return Optional.ofNullable(this.pattern.matchStartOfPath(request.pathContainer())).map(info -> new SubPathServerRequestWrapper(request, info, this.pattern));
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.path(this.pattern.getPatternString());
        }

        @Override
        public String toString() {
            return this.pattern.getPatternString();
        }

    }

    private static class HeadersPredicate implements RequestPredicate {

        private final Predicate<ServerRequest.Headers> headersPredicate;

        public HeadersPredicate(Predicate<ServerRequest.Headers> headersPredicate) {
            Assert.notNull(headersPredicate, "Predicate must not be null");
            this.headersPredicate = headersPredicate;
        }

        @Override
        public boolean test(ServerRequest request) {
            return this.headersPredicate.test(request.headers());
        }

        @Override
        public String toString() {
            return this.headersPredicate.toString();
        }

    }

    private static class ContentTypePredicate extends HeadersPredicate {

        private final Set<MediaType> mediaTypes;

        public ContentTypePredicate(MediaType... mediaTypes) {
            this(new HashSet<>(Arrays.asList(mediaTypes)));
        }

        private ContentTypePredicate(Set<MediaType> mediaTypes) {
            super(headers -> {
                MediaType contentType = headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
                boolean match = mediaTypes.stream().anyMatch(mediaType -> mediaType.includes(contentType));
                traceMatch("Content-Type", mediaTypes, contentType, match);
                return match;
            });
            this.mediaTypes = mediaTypes;
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.header(HttpHeaders.CONTENT_TYPE, (this.mediaTypes.size() == 1) ? this.mediaTypes.iterator().next().toString() : this.mediaTypes.toString());
        }

        @Override
        public String toString() {
            return String.format("Content-Type: %s", (this.mediaTypes.size() == 1) ? this.mediaTypes.iterator().next().toString() : this.mediaTypes.toString());
        }

    }

    private static class AcceptPredicate extends HeadersPredicate {

        private final Set<MediaType> mediaTypes;

        public AcceptPredicate(MediaType... mediaTypes) {
            this(new HashSet<>(Arrays.asList(mediaTypes)));
        }

        private AcceptPredicate(Set<MediaType> mediaTypes) {
            super(headers -> {
                List<MediaType> acceptedMediaTypes = acceptedMediaTypes(headers);
                boolean match = acceptedMediaTypes.stream().anyMatch(acceptedMediaType -> mediaTypes.stream().anyMatch(acceptedMediaType::isCompatibleWith));
                traceMatch("Accept", mediaTypes, acceptedMediaTypes, match);
                return match;
            });
            this.mediaTypes = mediaTypes;
        }

        @NonNull
        private static List<MediaType> acceptedMediaTypes(ServerRequest.Headers headers) {
            List<MediaType> acceptedMediaTypes = headers.accept();
            if (acceptedMediaTypes.isEmpty()) {
                acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
            } else {
                MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
            }
            return acceptedMediaTypes;
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.header(HttpHeaders.ACCEPT, (this.mediaTypes.size() == 1) ? this.mediaTypes.iterator().next().toString() : this.mediaTypes.toString());
        }

        @Override
        public String toString() {
            return String.format("Accept: %s", (this.mediaTypes.size() == 1) ? this.mediaTypes.iterator().next().toString() : this.mediaTypes.toString());
        }

    }

    private static class PathExtensionPredicate implements RequestPredicate {

        private final Predicate<String> extensionPredicate;

        @Nullable
        private final String extension;

        public PathExtensionPredicate(Predicate<String> extensionPredicate) {
            Assert.notNull(extensionPredicate, "Predicate must not be null");
            this.extensionPredicate = extensionPredicate;
            this.extension = null;
        }

        public PathExtensionPredicate(String extension) {
            Assert.notNull(extension, "Extension must not be null");
            this.extensionPredicate = s -> {
                boolean match = extension.equalsIgnoreCase(s);
                traceMatch("Extension", extension, s, match);
                return match;
            };
            this.extension = extension;
        }

        @Override
        public boolean test(ServerRequest request) {
            String pathExtension = UriUtils.extractFileExtension(request.path());
            return this.extensionPredicate.test(pathExtension);
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.pathExtension((this.extension != null) ? this.extension : this.extensionPredicate.toString());
        }

        @Override
        public String toString() {
            return String.format("*.%s", (this.extension != null) ? this.extension : this.extensionPredicate);
        }

    }

    private static class ParamPredicate implements RequestPredicate {

        private final String name;

        private final Predicate<String> valuePredicate;

        @Nullable
        private final String value;

        public ParamPredicate(String name, Predicate<String> valuePredicate) {
            Assert.notNull(name, "Name must not be null");
            Assert.notNull(valuePredicate, "Predicate must not be null");
            this.name = name;
            this.valuePredicate = valuePredicate;
            this.value = null;
        }

        public ParamPredicate(String name, String value) {
            Assert.notNull(name, "Name must not be null");
            Assert.notNull(value, "Value must not be null");
            this.name = name;
            this.valuePredicate = value::equals;
            this.value = value;
        }

        @Override
        public boolean test(ServerRequest request) {
            Optional<String> s = request.param(this.name);
            return s.filter(this.valuePredicate).isPresent();
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.param(this.name, (this.value != null) ? this.value : this.valuePredicate.toString());
        }

        @Override
        public String toString() {
            return String.format("?%s %s", this.name, (this.value != null) ? this.value : this.valuePredicate);
        }

    }

    static class AndRequestPredicate implements RequestPredicate {

        private final RequestPredicate left;

        private final RequestPredicate right;

        public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
            Assert.notNull(left, "Left RequestPredicate must not be null");
            Assert.notNull(right, "Right RequestPredicate must not be null");
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean test(ServerRequest request) {
            Map<String, Object> oldAttributes = new HashMap<>(request.attributes());
            if (this.left.test(request) && this.right.test(request)) {
                return true;
            }
            restoreAttributes(request, oldAttributes);
            return false;
        }

        @Override
        public Optional<ServerRequest> nest(ServerRequest request) {
            return this.left.nest(request).flatMap(this.right::nest);
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.startAnd();
            this.left.accept(visitor);
            visitor.and();
            this.right.accept(visitor);
            visitor.endAnd();
        }

        @Override
        public String toString() {
            return String.format("(%s && %s)", this.left, this.right);
        }

    }

    static class NegateRequestPredicate implements RequestPredicate {
        private final RequestPredicate delegate;

        public NegateRequestPredicate(RequestPredicate delegate) {
            Assert.notNull(delegate, "Delegate must not be null");
            this.delegate = delegate;
        }

        @Override
        public boolean test(ServerRequest request) {
            Map<String, Object> oldAttributes = new HashMap<>(request.attributes());
            boolean result = !this.delegate.test(request);
            if (!result) {
                restoreAttributes(request, oldAttributes);
            }
            return result;
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.startNegate();
            this.delegate.accept(visitor);
            visitor.endNegate();
        }

        @Override
        public String toString() {
            return "!" + this.delegate.toString();
        }

    }

    static class OrRequestPredicate implements RequestPredicate {

        private final RequestPredicate left;

        private final RequestPredicate right;

        public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
            Assert.notNull(left, "Left RequestPredicate must not be null");
            Assert.notNull(right, "Right RequestPredicate must not be null");
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean test(ServerRequest request) {
            Map<String, Object> oldAttributes = new HashMap<>(request.attributes());
            if (this.left.test(request)) {
                return true;
            } else {
                restoreAttributes(request, oldAttributes);
                if (this.right.test(request)) {
                    return true;
                }
            }
            restoreAttributes(request, oldAttributes);
            return false;
        }

        @Override
        public Optional<ServerRequest> nest(ServerRequest request) {
            Optional<ServerRequest> leftResult = this.left.nest(request);
            if (leftResult.isPresent()) {
                return leftResult;
            } else {
                return this.right.nest(request);
            }
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.startOr();
            this.left.accept(visitor);
            visitor.or();
            this.right.accept(visitor);
            visitor.endOr();
        }

        @Override
        public String toString() {
            return String.format("(%s || %s)", this.left, this.right);
        }

    }

    private static class SubPathServerRequestWrapper implements ServerRequest {

        private final ServerRequest request;

        private final PathContainer pathContainer;

        private final Map<String, Object> attributes;

        public SubPathServerRequestWrapper(ServerRequest request, PathPattern.PathRemainingMatchInfo info, PathPattern pattern) {
            this.request = request;
            this.pathContainer = new SubPathContainer(info.getPathRemaining());
            this.attributes = mergeAttributes(request, info.getUriVariables(), pattern);
        }

        private static Map<String, Object> mergeAttributes(ServerRequest request, Map<String, String> pathVariables, PathPattern pattern) {
            Map<String, Object> result = new ConcurrentHashMap<>(request.attributes());
            result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mergePathVariables(request.pathVariables(), pathVariables));
            pattern = mergePatterns((PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE), pattern);
            result.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
            return result;
        }

        @Override
        public HttpMethod method() {
            return this.request.method();
        }

        @Override
        public String methodName() {
            return this.request.methodName();
        }

        @Override
        public URI uri() {
            return this.request.uri();
        }

        @Override
        public UriBuilder uriBuilder() {
            return this.request.uriBuilder();
        }

        @Override
        public String path() {
            return this.pathContainer.value();
        }

        @Override
        public PathContainer pathContainer() {
            return this.pathContainer;
        }

        @Override
        public Headers headers() {
            return this.request.headers();
        }

        @Override
        public MultiValueMap<String, Cookie> cookies() {
            return this.request.cookies();
        }

        @Override
        public Optional<InetSocketAddress> remoteAddress() {
            return this.request.remoteAddress();
        }

        @Override
        public List<HttpMessageConverter<?>> messageConverters() {
            return this.request.messageConverters();
        }

        @Override
        public <T> T body(Class<T> bodyType) throws ServletException, IOException {
            return this.request.body(bodyType);
        }

        @Override
        public <T> T body(ParameterizedTypeReference<T> bodyType) throws ServletException, IOException {
            return this.request.body(bodyType);
        }

        @Override
        public Optional<Object> attribute(String name) {
            return this.request.attribute(name);
        }

        @Override
        public Map<String, Object> attributes() {
            return this.attributes;
        }

        @Override
        public Optional<String> param(String name) {
            return this.request.param(name);
        }

        @Override
        public MultiValueMap<String, String> params() {
            return this.request.params();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, String> pathVariables() {
            return (Map<String, String>) this.attributes.getOrDefault(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
        }

        @Override
        public HttpSession session() {
            return this.request.session();
        }

        @Override
        public Optional<Principal> principal() {
            return this.request.principal();
        }

        @Override
        public HttpServletRequest servletRequest() {
            return this.request.servletRequest();
        }

        @Override
        public String toString() {
            return method() + " " + path();
        }

        private static class SubPathContainer implements PathContainer {

            private static final PathContainer.Separator SEPARATOR = () -> "/";

            private final String value;

            private final List<Element> elements;

            public SubPathContainer(PathContainer original) {
                this.value = prefixWithSlash(original.value());
                this.elements = prependWithSeparator(original.elements());
            }

            private static String prefixWithSlash(String path) {
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                return path;
            }

            private static List<Element> prependWithSeparator(List<Element> elements) {
                List<Element> result = new ArrayList<>(elements);
                if (result.isEmpty() || !(result.get(0) instanceof Separator)) {
                    result.add(0, SEPARATOR);
                }
                return Collections.unmodifiableList(result);
            }

            @Override
            public String value() {
                return this.value;
            }

            @Override
            public List<Element> elements() {
                return this.elements;
            }

        }

    }

}
