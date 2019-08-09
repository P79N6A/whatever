package org.springframework.web.servlet.function;

import org.springframework.util.Assert;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface HandlerFilterFunction<T extends ServerResponse, R extends ServerResponse> {

    R filter(ServerRequest request, HandlerFunction<T> next) throws Exception;

    default HandlerFilterFunction<T, R> andThen(HandlerFilterFunction<T, T> after) {
        Assert.notNull(after, "HandlerFilterFunction must not be null");
        return (request, next) -> {
            HandlerFunction<T> nextHandler = handlerRequest -> after.filter(handlerRequest, next);
            return filter(request, nextHandler);
        };
    }

    default HandlerFunction<R> apply(HandlerFunction<T> handler) {
        Assert.notNull(handler, "HandlerFunction must not be null");
        return request -> this.filter(request, handler);
    }

    static <T extends ServerResponse> HandlerFilterFunction<T, T> ofRequestProcessor(Function<ServerRequest, ServerRequest> requestProcessor) {
        Assert.notNull(requestProcessor, "Function must not be null");
        return (request, next) -> next.handle(requestProcessor.apply(request));
    }

    static <T extends ServerResponse, R extends ServerResponse> HandlerFilterFunction<T, R> ofResponseProcessor(BiFunction<ServerRequest, T, R> responseProcessor) {
        Assert.notNull(responseProcessor, "Function must not be null");
        return (request, next) -> responseProcessor.apply(request, next.handle(request));
    }

    static <T extends ServerResponse> HandlerFilterFunction<T, T> ofErrorHandler(Predicate<Throwable> predicate, BiFunction<Throwable, ServerRequest, T> errorHandler) {
        Assert.notNull(predicate, "Predicate must not be null");
        Assert.notNull(errorHandler, "ErrorHandler must not be null");
        return (request, next) -> {
            try {
                T t = next.handle(request);
                if (t instanceof DefaultServerResponseBuilder.AbstractServerResponse) {
                    ((DefaultServerResponseBuilder.AbstractServerResponse) t).addErrorHandler(predicate, errorHandler);
                }
                return t;
            } catch (Throwable throwable) {
                if (predicate.test(throwable)) {
                    return errorHandler.apply(throwable, request);
                } else {
                    throw throwable;
                }
            }
        };
    }

}
