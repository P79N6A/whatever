package org.springframework.web.servlet.mvc.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.*;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

public abstract class AbstractMessageConverterMethodArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Set<HttpMethod> SUPPORTED_METHODS = EnumSet.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    private static final Object NO_VALUE = new Object();

    protected final Log logger = LogFactory.getLog(getClass());

    protected final List<HttpMessageConverter<?>> messageConverters;

    protected final List<MediaType> allSupportedMediaTypes;

    private final RequestResponseBodyAdviceChain advice;

    public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters) {
        this(converters, null);
    }

    public AbstractMessageConverterMethodArgumentResolver(List<HttpMessageConverter<?>> converters, @Nullable List<Object> requestResponseBodyAdvice) {
        Assert.notEmpty(converters, "'messageConverters' must not be empty");
        this.messageConverters = converters;
        this.allSupportedMediaTypes = getAllSupportedMediaTypes(converters);
        this.advice = new RequestResponseBodyAdviceChain(requestResponseBodyAdvice);
    }

    private static List<MediaType> getAllSupportedMediaTypes(List<HttpMessageConverter<?>> messageConverters) {
        Set<MediaType> allSupportedMediaTypes = new LinkedHashSet<>();
        for (HttpMessageConverter<?> messageConverter : messageConverters) {
            allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
        }
        List<MediaType> result = new ArrayList<>(allSupportedMediaTypes);
        MediaType.sortBySpecificity(result);
        return Collections.unmodifiableList(result);
    }

    RequestResponseBodyAdviceChain getAdvice() {
        return this.advice;
    }

    @Nullable
    protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter parameter, Type paramType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
        HttpInputMessage inputMessage = createInputMessage(webRequest);
        return readWithMessageConverters(inputMessage, parameter, paramType);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {
        MediaType contentType;
        boolean noContentType = false;
        try {
            contentType = inputMessage.getHeaders().getContentType();
        } catch (InvalidMediaTypeException ex) {
            throw new HttpMediaTypeNotSupportedException(ex.getMessage());
        }
        if (contentType == null) {
            noContentType = true;
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        Class<?> contextClass = parameter.getContainingClass();
        Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);
        if (targetClass == null) {
            ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);
            targetClass = (Class<T>) resolvableType.resolve();
        }
        HttpMethod httpMethod = (inputMessage instanceof HttpRequest ? ((HttpRequest) inputMessage).getMethod() : null);
        Object body = NO_VALUE;
        EmptyBodyCheckingHttpInputMessage message;
        try {
            message = new EmptyBodyCheckingHttpInputMessage(inputMessage);
            for (HttpMessageConverter<?> converter : this.messageConverters) {
                Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();
                GenericHttpMessageConverter<?> genericConverter = (converter instanceof GenericHttpMessageConverter ? (GenericHttpMessageConverter<?>) converter : null);
                if (genericConverter != null ? genericConverter.canRead(targetType, contextClass, contentType) : (targetClass != null && converter.canRead(targetClass, contentType))) {
                    if (message.hasBody()) {
                        HttpInputMessage msgToUse = getAdvice().beforeBodyRead(message, parameter, targetType, converterType);
                        body = (genericConverter != null ? genericConverter.read(targetType, contextClass, msgToUse) : ((HttpMessageConverter<T>) converter).read(targetClass, msgToUse));
                        body = getAdvice().afterBodyRead(body, msgToUse, parameter, targetType, converterType);
                    } else {
                        body = getAdvice().handleEmptyBody(null, message, parameter, targetType, converterType);
                    }
                    break;
                }
            }
        } catch (IOException ex) {
            throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);
        }
        if (body == NO_VALUE) {
            if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) || (noContentType && !message.hasBody())) {
                return null;
            }
            throw new HttpMediaTypeNotSupportedException(contentType, this.allSupportedMediaTypes);
        }
        MediaType selectedContentType = contentType;
        Object theBody = body;
        LogFormatUtils.traceDebug(logger, traceOn -> {
            String formatted = LogFormatUtils.formatValue(theBody, !traceOn);
            return "Read \"" + selectedContentType + "\" to [" + formatted + "]";
        });
        return body;
    }

    protected ServletServerHttpRequest createInputMessage(NativeWebRequest webRequest) {
        HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        Assert.state(servletRequest != null, "No HttpServletRequest");
        return new ServletServerHttpRequest(servletRequest);
    }

    protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
        Annotation[] annotations = parameter.getParameterAnnotations();
        for (Annotation ann : annotations) {
            Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
            if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
                Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
                Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[]{hints});
                binder.validate(validationHints);
                break;
            }
        }
    }

    protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
        int i = parameter.getParameterIndex();
        Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
        boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
        return !hasBindingResult;
    }

    @Nullable
    protected Object adaptArgumentIfNecessary(@Nullable Object arg, MethodParameter parameter) {
        if (parameter.getParameterType() == Optional.class) {
            if (arg == null || (arg instanceof Collection && ((Collection<?>) arg).isEmpty()) || (arg instanceof Object[] && ((Object[]) arg).length == 0)) {
                return Optional.empty();
            } else {
                return Optional.of(arg);
            }
        }
        return arg;
    }

    private static class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {

        private final HttpHeaders headers;

        @Nullable
        private final InputStream body;

        public EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
            this.headers = inputMessage.getHeaders();
            InputStream inputStream = inputMessage.getBody();
            if (inputStream.markSupported()) {
                inputStream.mark(1);
                this.body = (inputStream.read() != -1 ? inputStream : null);
                inputStream.reset();
            } else {
                PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
                int b = pushbackInputStream.read();
                if (b == -1) {
                    this.body = null;
                } else {
                    this.body = pushbackInputStream;
                    pushbackInputStream.unread(b);
                }
            }
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        @Override
        public InputStream getBody() {
            return (this.body != null ? this.body : StreamUtils.emptyInput());
        }

        public boolean hasBody() {
            return (this.body != null);
        }

    }

}
