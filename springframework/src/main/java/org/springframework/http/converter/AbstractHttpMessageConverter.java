package org.springframework.http.converter;

import org.apache.commons.logging.Log;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractHttpMessageConverter<T> implements HttpMessageConverter<T> {

    protected final Log logger = HttpLogging.forLogName(getClass());

    private List<MediaType> supportedMediaTypes = Collections.emptyList();

    @Nullable
    private Charset defaultCharset;

    protected AbstractHttpMessageConverter() {
    }

    protected AbstractHttpMessageConverter(MediaType supportedMediaType) {
        setSupportedMediaTypes(Collections.singletonList(supportedMediaType));
    }

    protected AbstractHttpMessageConverter(MediaType... supportedMediaTypes) {
        setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
    }

    protected AbstractHttpMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
        this.defaultCharset = defaultCharset;
        setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
    }

    public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
        Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
        this.supportedMediaTypes = new ArrayList<>(supportedMediaTypes);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(this.supportedMediaTypes);
    }

    public void setDefaultCharset(@Nullable Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
    }

    @Nullable
    public Charset getDefaultCharset() {
        return this.defaultCharset;
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return supports(clazz) && canRead(mediaType);
    }

    protected boolean canRead(@Nullable MediaType mediaType) {
        if (mediaType == null) {
            return true;
        }
        for (MediaType supportedMediaType : getSupportedMediaTypes()) {
            if (supportedMediaType.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return supports(clazz) && canWrite(mediaType);
    }

    protected boolean canWrite(@Nullable MediaType mediaType) {
        if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
            return true;
        }
        for (MediaType supportedMediaType : getSupportedMediaTypes()) {
            if (supportedMediaType.isCompatibleWith(mediaType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final T read(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return readInternal(clazz, inputMessage);
    }

    @Override
    public final void write(final T t, @Nullable MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final HttpHeaders headers = outputMessage.getHeaders();
        addDefaultHeaders(headers, t, contentType);
        if (outputMessage instanceof StreamingHttpOutputMessage) {
            StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
            streamingOutputMessage.setBody(outputStream -> writeInternal(t, new HttpOutputMessage() {
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
            writeInternal(t, outputMessage);
            outputMessage.getBody().flush();
        }
    }

    protected void addDefaultHeaders(HttpHeaders headers, T t, @Nullable MediaType contentType) throws IOException {
        if (headers.getContentType() == null) {
            MediaType contentTypeToUse = contentType;
            if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
                contentTypeToUse = getDefaultContentType(t);
            } else if (MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
                MediaType mediaType = getDefaultContentType(t);
                contentTypeToUse = (mediaType != null ? mediaType : contentTypeToUse);
            }
            if (contentTypeToUse != null) {
                if (contentTypeToUse.getCharset() == null) {
                    Charset defaultCharset = getDefaultCharset();
                    if (defaultCharset != null) {
                        contentTypeToUse = new MediaType(contentTypeToUse, defaultCharset);
                    }
                }
                headers.setContentType(contentTypeToUse);
            }
        }
        if (headers.getContentLength() < 0 && !headers.containsKey(HttpHeaders.TRANSFER_ENCODING)) {
            Long contentLength = getContentLength(t, headers.getContentType());
            if (contentLength != null) {
                headers.setContentLength(contentLength);
            }
        }
    }

    @Nullable
    protected MediaType getDefaultContentType(T t) throws IOException {
        List<MediaType> mediaTypes = getSupportedMediaTypes();
        return (!mediaTypes.isEmpty() ? mediaTypes.get(0) : null);
    }

    @Nullable
    protected Long getContentLength(T t, @Nullable MediaType contentType) throws IOException {
        return null;
    }

    protected abstract boolean supports(Class<?> clazz);

    protected abstract T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException;

    protected abstract void writeInternal(T t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException;

}
