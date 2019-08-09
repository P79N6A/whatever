package org.springframework.web.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class HttpMessageConverterExtractor<T> implements ResponseExtractor<T> {

    private final Type responseType;

    @Nullable
    private final Class<T> responseClass;

    private final List<HttpMessageConverter<?>> messageConverters;

    private final Log logger;

    public HttpMessageConverterExtractor(Class<T> responseType, List<HttpMessageConverter<?>> messageConverters) {
        this((Type) responseType, messageConverters);
    }

    public HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters) {
        this(responseType, messageConverters, LogFactory.getLog(HttpMessageConverterExtractor.class));
    }

    @SuppressWarnings("unchecked")
    HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters, Log logger) {
        Assert.notNull(responseType, "'responseType' must not be null");
        Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
        this.responseType = responseType;
        this.responseClass = (responseType instanceof Class ? (Class<T>) responseType : null);
        this.messageConverters = messageConverters;
        this.logger = logger;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes", "resource"})
    public T extractData(ClientHttpResponse response) throws IOException {
        MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);
        if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
            return null;
        }
        MediaType contentType = getContentType(responseWrapper);
        try {
            for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
                if (messageConverter instanceof GenericHttpMessageConverter) {
                    GenericHttpMessageConverter<?> genericMessageConverter = (GenericHttpMessageConverter<?>) messageConverter;
                    if (genericMessageConverter.canRead(this.responseType, null, contentType)) {
                        if (logger.isDebugEnabled()) {
                            ResolvableType resolvableType = ResolvableType.forType(this.responseType);
                            logger.debug("Reading to [" + resolvableType + "]");
                        }
                        return (T) genericMessageConverter.read(this.responseType, null, responseWrapper);
                    }
                }
                if (this.responseClass != null) {
                    if (messageConverter.canRead(this.responseClass, contentType)) {
                        if (logger.isDebugEnabled()) {
                            String className = this.responseClass.getName();
                            logger.debug("Reading to [" + className + "] as \"" + contentType + "\"");
                        }
                        return (T) messageConverter.read((Class) this.responseClass, responseWrapper);
                    }
                }
            }
        } catch (IOException | HttpMessageNotReadableException ex) {
            throw new RestClientException("Error while extracting response for type [" + this.responseType + "] and content type [" + contentType + "]", ex);
        }
        throw new RestClientException("Could not extract response: no suitable HttpMessageConverter found " + "for response type [" + this.responseType + "] and content type [" + contentType + "]");
    }

    @Nullable
    protected MediaType getContentType(ClientHttpResponse response) {
        MediaType contentType = response.getHeaders().getContentType();
        if (contentType == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("No content-type, using 'application/octet-stream'");
            }
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }
        return contentType;
    }

}
