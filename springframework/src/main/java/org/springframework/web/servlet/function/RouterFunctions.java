package org.springframework.web.servlet.function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.function.*;

public abstract class RouterFunctions {

    private static final Log logger = LogFactory.getLog(RouterFunctions.class);

    public static final String REQUEST_ATTRIBUTE = RouterFunctions.class.getName() + ".request";

    public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE = RouterFunctions.class.getName() + ".uriTemplateVariables";

    public static final String MATCHING_PATTERN_ATTRIBUTE = RouterFunctions.class.getName() + ".matchingPattern";

    public static Builder route() {
        return new RouterFunctionBuilder();
    }

    public static <T extends ServerResponse> RouterFunction<T> route(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
        return new DefaultRouterFunction<>(predicate, handlerFunction);
    }

    public static <T extends ServerResponse> RouterFunction<T> nest(RequestPredicate predicate, RouterFunction<T> routerFunction) {
        return new DefaultNestedRouterFunction<>(predicate, routerFunction);
    }

    public static RouterFunction<ServerResponse> resources(String pattern, Resource location) {
        return resources(resourceLookupFunction(pattern, location));
    }

    public static Function<ServerRequest, Optional<Resource>> resourceLookupFunction(String pattern, Resource location) {
        return new PathResourceLookupFunction(pattern, location);
    }

    public static RouterFunction<ServerResponse> resources(Function<ServerRequest, Optional<Resource>> lookupFunction) {
        return new ResourcesRouterFunction(lookupFunction);
    }

    @SuppressWarnings("unchecked")
    static <T extends ServerResponse> HandlerFunction<T> cast(HandlerFunction<?> handlerFunction) {
        return (HandlerFunction<T>) handlerFunction;
    }

    public interface Builder {

        Builder GET(String pattern, HandlerFunction<ServerResponse> handlerFunction);

        Builder GET(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder HEAD(String pattern, HandlerFunction<ServerResponse> handlerFunction);

        Builder HEAD(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder POST(String pattern, HandlerFunction<ServerResponse> handlerFunction);

        Builder POST(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder PUT(String pattern, HandlerFunction<ServerResponse> handlerFunction);

        Builder PUT(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder PATCH(String pattern, HandlerFunction<ServerResponse> handlerFunction);

        Builder PATCH(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder DELETE(String pattern, HandlerFunction<ServerResponse> handlerFunction);

        Builder DELETE(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder OPTIONS(String pattern, HandlerFunction<ServerResponse> handlerFunction);

        Builder route(RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder OPTIONS(String pattern, RequestPredicate predicate, HandlerFunction<ServerResponse> handlerFunction);

        Builder add(RouterFunction<ServerResponse> routerFunction);

        Builder resources(String pattern, Resource location);

        Builder resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

        Builder nest(RequestPredicate predicate, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

        Builder nest(RequestPredicate predicate, Consumer<Builder> builderConsumer);

        Builder path(String pattern, Supplier<RouterFunction<ServerResponse>> routerFunctionSupplier);

        Builder path(String pattern, Consumer<Builder> builderConsumer);

        Builder filter(HandlerFilterFunction<ServerResponse, ServerResponse> filterFunction);

        Builder before(Function<ServerRequest, ServerRequest> requestProcessor);

        Builder after(BiFunction<ServerRequest, ServerResponse, ServerResponse> responseProcessor);

        Builder onError(Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

        Builder onError(Class<? extends Throwable> exceptionType, BiFunction<Throwable, ServerRequest, ServerResponse> responseProvider);

        RouterFunction<ServerResponse> build();

    }

    public interface Visitor {

        void startNested(RequestPredicate predicate);

        void endNested(RequestPredicate predicate);

        void route(RequestPredicate predicate, HandlerFunction<?> handlerFunction);

        void resources(Function<ServerRequest, Optional<Resource>> lookupFunction);

        void unknown(RouterFunction<?> routerFunction);

    }

    private abstract static class AbstractRouterFunction<T extends ServerResponse> implements RouterFunction<T> {

        @Override
        public String toString() {
            ToStringVisitor visitor = new ToStringVisitor();
            accept(visitor);
            return visitor.toString();
        }

    }

    static final class SameComposedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

        private final RouterFunction<T> first;

        private final RouterFunction<T> second;

        public SameComposedRouterFunction(RouterFunction<T> first, RouterFunction<T> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public Optional<HandlerFunction<T>> route(ServerRequest request) {
            Optional<HandlerFunction<T>> firstRoute = this.first.route(request);
            if (firstRoute.isPresent()) {
                return firstRoute;
            } else {
                return this.second.route(request);
            }
        }

        @Override
        public void accept(Visitor visitor) {
            this.first.accept(visitor);
            this.second.accept(visitor);
        }

    }

    static final class DifferentComposedRouterFunction extends AbstractRouterFunction<ServerResponse> {

        private final RouterFunction<?> first;

        private final RouterFunction<?> second;

        public DifferentComposedRouterFunction(RouterFunction<?> first, RouterFunction<?> second) {
            this.first = first;
            this.second = second;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
            Optional<? extends HandlerFunction<?>> firstRoute = this.first.route(request);
            if (firstRoute.isPresent()) {
                return (Optional<HandlerFunction<ServerResponse>>) firstRoute;
            } else {
                Optional<? extends HandlerFunction<?>> secondRoute = this.second.route(request);
                return (Optional<HandlerFunction<ServerResponse>>) secondRoute;
            }
        }

        @Override
        public void accept(Visitor visitor) {
            this.first.accept(visitor);
            this.second.accept(visitor);
        }

    }

    static final class FilteredRouterFunction<T extends ServerResponse, S extends ServerResponse> implements RouterFunction<S> {

        private final RouterFunction<T> routerFunction;

        private final HandlerFilterFunction<T, S> filterFunction;

        public FilteredRouterFunction(RouterFunction<T> routerFunction, HandlerFilterFunction<T, S> filterFunction) {
            this.routerFunction = routerFunction;
            this.filterFunction = filterFunction;
        }

        @Override
        public Optional<HandlerFunction<S>> route(ServerRequest request) {
            return this.routerFunction.route(request).map(this.filterFunction::apply);
        }

        @Override
        public void accept(Visitor visitor) {
            this.routerFunction.accept(visitor);
        }

        @Override
        public String toString() {
            return this.routerFunction.toString();
        }

    }

    private static final class DefaultRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

        private final RequestPredicate predicate;

        private final HandlerFunction<T> handlerFunction;

        public DefaultRouterFunction(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
            Assert.notNull(predicate, "Predicate must not be null");
            Assert.notNull(handlerFunction, "HandlerFunction must not be null");
            this.predicate = predicate;
            this.handlerFunction = handlerFunction;
        }

        @Override
        public Optional<HandlerFunction<T>> route(ServerRequest request) {
            if (this.predicate.test(request)) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Predicate \"%s\" matches against \"%s\"", this.predicate, request));
                }
                return Optional.of(this.handlerFunction);
            } else {
                return Optional.empty();
            }
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.route(this.predicate, this.handlerFunction);
        }

    }

    private static final class DefaultNestedRouterFunction<T extends ServerResponse> extends AbstractRouterFunction<T> {

        private final RequestPredicate predicate;

        private final RouterFunction<T> routerFunction;

        public DefaultNestedRouterFunction(RequestPredicate predicate, RouterFunction<T> routerFunction) {
            Assert.notNull(predicate, "Predicate must not be null");
            Assert.notNull(routerFunction, "RouterFunction must not be null");
            this.predicate = predicate;
            this.routerFunction = routerFunction;
        }

        @Override
        public Optional<HandlerFunction<T>> route(ServerRequest serverRequest) {
            return this.predicate.nest(serverRequest).map(nestedRequest -> {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("Nested predicate \"%s\" matches against \"%s\"", this.predicate, serverRequest));
                }
                Optional<HandlerFunction<T>> result = this.routerFunction.route(nestedRequest);
                if (result.isPresent() && nestedRequest != serverRequest) {
                    serverRequest.attributes().clear();
                    serverRequest.attributes().putAll(nestedRequest.attributes());
                }
                return result;
            }).orElseGet(Optional::empty);
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.startNested(this.predicate);
            this.routerFunction.accept(visitor);
            visitor.endNested(this.predicate);
        }

    }

    private static class ResourcesRouterFunction extends AbstractRouterFunction<ServerResponse> {

        private final Function<ServerRequest, Optional<Resource>> lookupFunction;

        public ResourcesRouterFunction(Function<ServerRequest, Optional<Resource>> lookupFunction) {
            Assert.notNull(lookupFunction, "Function must not be null");
            this.lookupFunction = lookupFunction;
        }

        @Override
        public Optional<HandlerFunction<ServerResponse>> route(ServerRequest request) {
            return this.lookupFunction.apply(request).map(ResourceHandlerFunction::new);
        }

        @Override
        public void accept(Visitor visitor) {
            visitor.resources(this.lookupFunction);
        }

    }

}
