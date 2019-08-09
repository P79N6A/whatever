package org.springframework.web.servlet.function;

@FunctionalInterface
public interface HandlerFunction<T extends ServerResponse> {

    T handle(ServerRequest request) throws Exception;

}
