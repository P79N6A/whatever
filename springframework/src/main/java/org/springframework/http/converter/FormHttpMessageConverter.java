package org.springframework.http.converter;

import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;

import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FormHttpMessageConverter implements HttpMessageConverter<MultiValueMap<String, ?>> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final MediaType DEFAULT_FORM_DATA_MEDIA_TYPE = new MediaType(MediaType.APPLICATION_FORM_URLENCODED, DEFAULT_CHARSET);

    private List<MediaType> supportedMediaTypes = new ArrayList<>();

    private List<HttpMessageConverter<?>> partConverters = new ArrayList<>();

    private Charset charset = DEFAULT_CHARSET;

    @Nullable
    private Charset multipartCharset;

    public FormHttpMessageConverter() {
        this.supportedMediaTypes.add(MediaType.APPLICATION_FORM_URLENCODED);
        this.supportedMediaTypes.add(MediaType.MULTIPART_FORM_DATA);
        this.partConverters.add(new ByteArrayHttpMessageConverter());
        this.partConverters.add(new StringHttpMessageConverter());
        this.partConverters.add(new ResourceHttpMessageConverter());
        applyDefaultCharset();
    }

    public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
        this.supportedMediaTypes = supportedMediaTypes;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(this.supportedMediaTypes);
    }

    public void setPartConverters(List<HttpMessageConverter<?>> partConverters) {
        Assert.notEmpty(partConverters, "'partConverters' must not be empty");
        this.partConverters = partConverters;
    }

    public void addPartConverter(HttpMessageConverter<?> partConverter) {
        Assert.notNull(partConverter, "'partConverter' must not be null");
        this.partConverters.add(partConverter);
    }

    public void setCharset(@Nullable Charset charset) {
        if (charset != this.charset) {
            this.charset = (charset != null ? charset : DEFAULT_CHARSET);
            applyDefaultCharset();
        }
    }

    private void applyDefaultCharset() {
        for (HttpMessageConverter<?> candidate : this.partConverters) {
            if (candidate instanceof AbstractHttpMessageConverter) {
                AbstractHttpMessageConverter<?> converter = (AbstractHttpMessageConverter<?>) candidate;
                // Only override default charset if the converter operates with a charset to begin with...
                if (converter.getDefaultCharset() != null) {
                    converter.setDefaultCharset(this.charset);
                }
            }
        }
    }

    public void setMultipartCharset(Charset charset) {
        this.multipartCharset = charset;
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        if (!MultiValueMap.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (mediaType == null) {
            return true;
        }
        for (MediaType supportedMediaType : getSupportedMediaTypes()) {
            // We can't read multipart....
            if (!supportedMediaType.equals(MediaType.MULTIPART_FORM_DATA) && supportedMediaType.includes(mediaType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        if (!MultiValueMap.class.isAssignableFrom(clazz)) {
            return false;
        }
        if (mediaType == null || MediaType.ALL.equals(mediaType)) {
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
    public MultiValueMap<String, String> read(@Nullable Class<? extends MultiValueMap<String, ?>> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        MediaType contentType = inputMessage.getHeaders().getContentType();
        Charset charset = (contentType != null && contentType.getCharset() != null ? contentType.getCharset() : this.charset);
        String body = StreamUtils.copyToString(inputMessage.getBody(), charset);
        String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx == -1) {
                result.add(URLDecoder.decode(pair, charset.name()), null);
            } else {
                String name = URLDecoder.decode(pair.substring(0, idx), charset.name());
                String value = URLDecoder.decode(pair.substring(idx + 1), charset.name());
                result.add(name, value);
            }
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(MultiValueMap<String, ?> map, @Nullable MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        if (!isMultipart(map, contentType)) {
            writeForm((MultiValueMap<String, Object>) map, contentType, outputMessage);
        } else {
            writeMultipart((MultiValueMap<String, Object>) map, outputMessage);
        }
    }

    private boolean isMultipart(MultiValueMap<String, ?> map, @Nullable MediaType contentType) {
        if (contentType != null) {
            return MediaType.MULTIPART_FORM_DATA.includes(contentType);
        }
        for (String name : map.keySet()) {
            for (Object value : map.get(name)) {
                if (value != null && !(value instanceof String)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void writeForm(MultiValueMap<String, Object> formData, @Nullable MediaType contentType, HttpOutputMessage outputMessage) throws IOException {
        contentType = getMediaType(contentType);
        outputMessage.getHeaders().setContentType(contentType);
        Charset charset = contentType.getCharset();
        Assert.notNull(charset, "No charset"); // should never occur
        final byte[] bytes = serializeForm(formData, charset).getBytes(charset);
        outputMessage.getHeaders().setContentLength(bytes.length);
        if (outputMessage instanceof StreamingHttpOutputMessage) {
            StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
            streamingOutputMessage.setBody(outputStream -> StreamUtils.copy(bytes, outputStream));
        } else {
            StreamUtils.copy(bytes, outputMessage.getBody());
        }
    }

    private MediaType getMediaType(@Nullable MediaType mediaType) {
        if (mediaType == null) {
            return DEFAULT_FORM_DATA_MEDIA_TYPE;
        } else if (mediaType.getCharset() == null) {
            return new MediaType(mediaType, this.charset);
        } else {
            return mediaType;
        }
    }

    protected String serializeForm(MultiValueMap<String, Object> formData, Charset charset) {
        StringBuilder builder = new StringBuilder();
        formData.forEach((name, values) -> values.forEach(value -> {
            try {
                if (builder.length() != 0) {
                    builder.append('&');
                }
                builder.append(URLEncoder.encode(name, charset.name()));
                if (value != null) {
                    builder.append('=');
                    builder.append(URLEncoder.encode(String.valueOf(value), charset.name()));
                }
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException(ex);
            }
        }));
        return builder.toString();
    }

    private void writeMultipart(final MultiValueMap<String, Object> parts, HttpOutputMessage outputMessage) throws IOException {
        final byte[] boundary = generateMultipartBoundary();
        Map<String, String> parameters = new LinkedHashMap<>(2);
        if (!isFilenameCharsetSet()) {
            parameters.put("charset", this.charset.name());
        }
        parameters.put("boundary", new String(boundary, StandardCharsets.US_ASCII));
        MediaType contentType = new MediaType(MediaType.MULTIPART_FORM_DATA, parameters);
        HttpHeaders headers = outputMessage.getHeaders();
        headers.setContentType(contentType);
        if (outputMessage instanceof StreamingHttpOutputMessage) {
            StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
            streamingOutputMessage.setBody(outputStream -> {
                writeParts(outputStream, parts, boundary);
                writeEnd(outputStream, boundary);
            });
        } else {
            writeParts(outputMessage.getBody(), parts, boundary);
            writeEnd(outputMessage.getBody(), boundary);
        }
    }

    private boolean isFilenameCharsetSet() {
        return (this.multipartCharset != null);
    }

    private void writeParts(OutputStream os, MultiValueMap<String, Object> parts, byte[] boundary) throws IOException {
        for (Map.Entry<String, List<Object>> entry : parts.entrySet()) {
            String name = entry.getKey();
            for (Object part : entry.getValue()) {
                if (part != null) {
                    writeBoundary(os, boundary);
                    writePart(name, getHttpEntity(part), os);
                    writeNewLine(os);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void writePart(String name, HttpEntity<?> partEntity, OutputStream os) throws IOException {
        Object partBody = partEntity.getBody();
        if (partBody == null) {
            throw new IllegalStateException("Empty body for part '" + name + "': " + partEntity);
        }
        Class<?> partType = partBody.getClass();
        HttpHeaders partHeaders = partEntity.getHeaders();
        MediaType partContentType = partHeaders.getContentType();
        for (HttpMessageConverter<?> messageConverter : this.partConverters) {
            if (messageConverter.canWrite(partType, partContentType)) {
                Charset charset = isFilenameCharsetSet() ? StandardCharsets.US_ASCII : this.charset;
                HttpOutputMessage multipartMessage = new MultipartHttpOutputMessage(os, charset);
                multipartMessage.getHeaders().setContentDispositionFormData(name, getFilename(partBody));
                if (!partHeaders.isEmpty()) {
                    multipartMessage.getHeaders().putAll(partHeaders);
                }
                ((HttpMessageConverter<Object>) messageConverter).write(partBody, partContentType, multipartMessage);
                return;
            }
        }
        throw new HttpMessageNotWritableException("Could not write request: no suitable HttpMessageConverter " + "found for request type [" + partType.getName() + "]");
    }

    protected byte[] generateMultipartBoundary() {
        return MimeTypeUtils.generateMultipartBoundary();
    }

    protected HttpEntity<?> getHttpEntity(Object part) {
        return (part instanceof HttpEntity ? (HttpEntity<?>) part : new HttpEntity<>(part));
    }

    @Nullable
    protected String getFilename(Object part) {
        if (part instanceof Resource) {
            Resource resource = (Resource) part;
            String filename = resource.getFilename();
            if (filename != null && this.multipartCharset != null) {
                filename = MimeDelegate.encode(filename, this.multipartCharset.name());
            }
            return filename;
        } else {
            return null;
        }
    }

    private void writeBoundary(OutputStream os, byte[] boundary) throws IOException {
        os.write('-');
        os.write('-');
        os.write(boundary);
        writeNewLine(os);
    }

    private static void writeEnd(OutputStream os, byte[] boundary) throws IOException {
        os.write('-');
        os.write('-');
        os.write(boundary);
        os.write('-');
        os.write('-');
        writeNewLine(os);
    }

    private static void writeNewLine(OutputStream os) throws IOException {
        os.write('\r');
        os.write('\n');
    }

    private static class MultipartHttpOutputMessage implements HttpOutputMessage {

        private final OutputStream outputStream;

        private final Charset charset;

        private final HttpHeaders headers = new HttpHeaders();

        private boolean headersWritten = false;

        public MultipartHttpOutputMessage(OutputStream outputStream, Charset charset) {
            this.outputStream = outputStream;
            this.charset = charset;
        }

        @Override
        public HttpHeaders getHeaders() {
            return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
        }

        @Override
        public OutputStream getBody() throws IOException {
            writeHeaders();
            return this.outputStream;
        }

        private void writeHeaders() throws IOException {
            if (!this.headersWritten) {
                for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
                    byte[] headerName = getBytes(entry.getKey());
                    for (String headerValueString : entry.getValue()) {
                        byte[] headerValue = getBytes(headerValueString);
                        this.outputStream.write(headerName);
                        this.outputStream.write(':');
                        this.outputStream.write(' ');
                        this.outputStream.write(headerValue);
                        writeNewLine(this.outputStream);
                    }
                }
                writeNewLine(this.outputStream);
                this.headersWritten = true;
            }
        }

        private byte[] getBytes(String name) {
            return name.getBytes(this.charset);
        }

    }

    private static class MimeDelegate {

        public static String encode(String value, String charset) {
            try {
                return MimeUtility.encodeText(value, charset, null);
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException(ex);
            }
        }

    }

}
