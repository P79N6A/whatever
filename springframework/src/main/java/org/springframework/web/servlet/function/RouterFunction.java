package org.springframework.web.servlet.function;

import java.util.Optional;

@FunctionalInterface
public interface RouterFunction<T extends ServerResponse> {

    Optional<HandlerFunction<T>> route(ServerRequest request);

    default RouterFunction<T> and(RouterFunction<T> other) {
        return new RouterFunctions.SameComposedRouterFunction<>(this, other);
    }

    default RouterFunction<?> andOther(RouterFunction<?> other) {
        return new RouterFunctions.DifferentComposedRouterFunction(this, other);
    }

    default RouterFunction<T> andRoute(RequestPredicate predicate, HandlerFunction<T> handlerFunction) {
        return and(RouterFunctions.route(predicate, handlerFunction));
    }

    default RouterFunction<T> andNest(RequestPredicate predicate, RouterFunction<T> routerFunction) {
        return and(RouterFunctions.nest(predicate, routerFunction));
    }

    default <S extends ServerResponse> RouterFunction<S> filter(HandlerFilterFunction<T, S> filterFunction) {
        return new RouterFunctions.FilteredRouterFunction<>(this, filterFunction);
    }

    default void accept(RouterFunctions.Visitor visitor) {
        visitor.unknown(this);
    }

}
