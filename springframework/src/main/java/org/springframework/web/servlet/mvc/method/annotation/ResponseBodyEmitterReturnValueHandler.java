package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

public class ResponseBodyEmitterReturnValueHandler implements HandlerMethodReturnValueHandler {

    private final List<HttpMessageConverter<?>> messageConverters;

    private final ReactiveTypeHandler reactiveHandler;

    public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters) {
        Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
        this.messageConverters = messageConverters;
        this.reactiveHandler = new ReactiveTypeHandler();
    }

    public ResponseBodyEmitterReturnValueHandler(List<HttpMessageConverter<?>> messageConverters, ReactiveAdapterRegistry registry, TaskExecutor executor, ContentNegotiationManager manager) {
        Assert.notEmpty(messageConverters, "HttpMessageConverter List must not be empty");
        this.messageConverters = messageConverters;
        this.reactiveHandler = new ReactiveTypeHandler(registry, executor, manager);
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        Class<?> bodyType = ResponseEntity.class.isAssignableFrom(returnType.getParameterType()) ? ResolvableType.forMethodParameter(returnType).getGeneric().resolve() : returnType.getParameterType();
        return (bodyType != null && (ResponseBodyEmitter.class.isAssignableFrom(bodyType) || this.reactiveHandler.isReactiveType(bodyType)));
    }

    @Override
    @SuppressWarnings("resource")
    public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        if (returnValue == null) {
            mavContainer.setRequestHandled(true);
            return;
        }
        HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
        Assert.state(response != null, "No HttpServletResponse");
        ServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
        if (returnValue instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnValue;
            response.setStatus(responseEntity.getStatusCodeValue());
            outputMessage.getHeaders().putAll(responseEntity.getHeaders());
            returnValue = responseEntity.getBody();
            returnType = returnType.nested();
            if (returnValue == null) {
                mavContainer.setRequestHandled(true);
                outputMessage.flush();
                return;
            }
        }
        ServletRequest request = webRequest.getNativeRequest(ServletRequest.class);
        Assert.state(request != null, "No ServletRequest");
        ResponseBodyEmitter emitter;
        if (returnValue instanceof ResponseBodyEmitter) {
            emitter = (ResponseBodyEmitter) returnValue;
        } else {
            emitter = this.reactiveHandler.handleValue(returnValue, returnType, mavContainer, webRequest);
            if (emitter == null) {
                // Not streaming: write headers without committing response..
                outputMessage.getHeaders().forEach((headerName, headerValues) -> {
                    for (String headerValue : headerValues) {
                        response.addHeader(headerName, headerValue);
                    }
                });
                return;
            }
        }
        emitter.extendResponse(outputMessage);
        // At this point we know we're streaming..
        ShallowEtagHeaderFilter.disableContentCaching(request);
        // Wrap the response to ignore further header changes
        // Headers will be flushed at the first write
        outputMessage = new StreamingServletServerHttpResponse(outputMessage);
        DeferredResult<?> deferredResult = new DeferredResult<>(emitter.getTimeout());
        WebAsyncUtils.getAsyncManager(webRequest).startDeferredResultProcessing(deferredResult, mavContainer);
        HttpMessageConvertingHandler handler = new HttpMessageConvertingHandler(outputMessage, deferredResult);
        emitter.initialize(handler);
    }

    private class HttpMessageConvertingHandler implements ResponseBodyEmitter.Handler {

        private final ServerHttpResponse outputMessage;

        private final DeferredResult<?> deferredResult;

        public HttpMessageConvertingHandler(ServerHttpResponse outputMessage, DeferredResult<?> deferredResult) {
            this.outputMessage = outputMessage;
            this.deferredResult = deferredResult;
        }

        @Override
        public void send(Object data, @Nullable MediaType mediaType) throws IOException {
            sendInternal(data, mediaType);
        }

        @SuppressWarnings("unchecked")
        private <T> void sendInternal(T data, @Nullable MediaType mediaType) throws IOException {
            for (HttpMessageConverter<?> converter : ResponseBodyEmitterReturnValueHandler.this.messageConverters) {
                if (converter.canWrite(data.getClass(), mediaType)) {
                    ((HttpMessageConverter<T>) converter).write(data, mediaType, this.outputMessage);
                    this.outputMessage.flush();
                    return;
                }
            }
            throw new IllegalArgumentException("No suitable converter for " + data.getClass());
        }

        @Override
        public void complete() {
            try {
                this.outputMessage.flush();
                this.deferredResult.setResult(null);
            } catch (IOException ex) {
                this.deferredResult.setErrorResult(ex);
            }
        }

        @Override
        public void completeWithError(Throwable failure) {
            this.deferredResult.setErrorResult(failure);
        }

        @Override
        public void onTimeout(Runnable callback) {
            this.deferredResult.onTimeout(callback);
        }

        @Override
        public void onError(Consumer<Throwable> callback) {
            this.deferredResult.onError(callback);
        }

        @Override
        public void onCompletion(Runnable callback) {
            this.deferredResult.onCompletion(callback);
        }

    }

    private static class StreamingServletServerHttpResponse implements ServerHttpResponse {

        private final ServerHttpResponse delegate;

        private final HttpHeaders mutableHeaders = new HttpHeaders();

        public StreamingServletServerHttpResponse(ServerHttpResponse delegate) {
            this.delegate = delegate;
            this.mutableHeaders.putAll(delegate.getHeaders());
        }

        @Override
        public void setStatusCode(HttpStatus status) {
            this.delegate.setStatusCode(status);
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.mutableHeaders;
        }

        @Override
        public OutputStream getBody() throws IOException {
            return this.delegate.getBody();
        }

        @Override
        public void flush() throws IOException {
            this.delegate.flush();
        }

        @Override
        public void close() {
            this.delegate.close();
        }

    }

}
