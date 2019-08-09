package org.springframework.web.servlet.function;

import java.util.Optional;

@FunctionalInterface
public interface RequestPredicate {

    boolean test(ServerRequest request);

    default RequestPredicate and(RequestPredicate other) {
        return new RequestPredicates.AndRequestPredicate(this, other);
    }

    default RequestPredicate negate() {
        return new RequestPredicates.NegateRequestPredicate(this);
    }

    default RequestPredicate or(RequestPredicate other) {
        return new RequestPredicates.OrRequestPredicate(this, other);
    }

    default Optional<ServerRequest> nest(ServerRequest request) {
        return (test(request) ? Optional.of(request) : Optional.empty());
    }

    default void accept(RequestPredicates.Visitor visitor) {
        visitor.unknown(this);
    }

}
