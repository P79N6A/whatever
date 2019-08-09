package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.async.WebAsyncUtils;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.concurrent.Callable;

public class StreamingResponseBodyReturnValueHandler implements HandlerMethodReturnValueHandler {

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        if (StreamingResponseBody.class.isAssignableFrom(returnType.getParameterType())) {
            return true;
        } else if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric().resolve();
            return (bodyType != null && StreamingResponseBody.class.isAssignableFrom(bodyType));
        }
        return false;
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
            if (returnValue == null) {
                mavContainer.setRequestHandled(true);
                outputMessage.flush();
                return;
            }
        }
        ServletRequest request = webRequest.getNativeRequest(ServletRequest.class);
        Assert.state(request != null, "No ServletRequest");
        ShallowEtagHeaderFilter.disableContentCaching(request);
        Assert.isInstanceOf(StreamingResponseBody.class, returnValue, "StreamingResponseBody expected");
        StreamingResponseBody streamingBody = (StreamingResponseBody) returnValue;
        Callable<Void> callable = new StreamingResponseBodyTask(outputMessage.getBody(), streamingBody);
        WebAsyncUtils.getAsyncManager(webRequest).startCallableProcessing(callable, mavContainer);
    }

    private static class StreamingResponseBodyTask implements Callable<Void> {

        private final OutputStream outputStream;

        private final StreamingResponseBody streamingBody;

        public StreamingResponseBodyTask(OutputStream outputStream, StreamingResponseBody streamingBody) {
            this.outputStream = outputStream;
            this.streamingBody = streamingBody;
        }

        @Override
        public Void call() throws Exception {
            this.streamingBody.writeTo(this.outputStream);
            this.outputStream.flush();
            return null;
        }

    }

}
