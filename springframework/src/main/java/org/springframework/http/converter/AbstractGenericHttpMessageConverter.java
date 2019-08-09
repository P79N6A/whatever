package org.springframework.http.converter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

public abstract class AbstractGenericHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> implements GenericHttpMessageConverter<T> {

    protected AbstractGenericHttpMessageConverter() {
    }

    protected AbstractGenericHttpMessageConverter(MediaType supportedMediaType) {
        super(supportedMediaType);
    }

    protected AbstractGenericHttpMessageConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
        return (type instanceof Class ? canRead((Class<?>) type, mediaType) : canRead(mediaType));
    }

    @Override
    public boolean canWrite(@Nullable Type type, Class<?> clazz, @Nullable MediaType mediaType) {
        return canWrite(clazz, mediaType);
    }

    public final void write(final T t, @Nullable final Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final HttpHeaders headers = outputMessage.getHeaders();
        addDefaultHeaders(headers, t, contentType);
        if (outputMessage instanceof StreamingHttpOutputMessage) {
            StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
            streamingOutputMessage.setBody(outputStream -> writeInternal(t, type, new HttpOutputMessage() {
                @Override
                public OutputStream getBody() {
                    return outputStream;
                }

                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }
            }));
        } else {
            writeInternal(t, type, outputMessage);
            outputMessage.getBody().flush();
        }
    }

    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        writeInternal(t, null, outputMessage);
    }

    protected abstract void writeInternal(T t, @Nullable Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException;

}
