package org.springframework.http.codec;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FormHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<MultiValueMap<String, String>> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private static final ResolvableType MULTIVALUE_STRINGS_TYPE = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

    private Charset defaultCharset = DEFAULT_CHARSET;

    public void setDefaultCharset(Charset charset) {
        Assert.notNull(charset, "Charset must not be null");
        this.defaultCharset = charset;
    }

    public Charset getDefaultCharset() {
        return this.defaultCharset;
    }

    @Override
    public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
        boolean multiValueUnresolved = elementType.hasUnresolvableGenerics() && MultiValueMap.class.isAssignableFrom(elementType.toClass());
        return ((MULTIVALUE_STRINGS_TYPE.isAssignableFrom(elementType) || multiValueUnresolved) && (mediaType == null || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)));
    }

    @Override
    public Flux<MultiValueMap<String, String>> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        return Flux.from(readMono(elementType, message, hints));
    }

    @Override
    public Mono<MultiValueMap<String, String>> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        MediaType contentType = message.getHeaders().getContentType();
        Charset charset = getMediaTypeCharset(contentType);
        return DataBufferUtils.join(message.getBody()).map(buffer -> {
            CharBuffer charBuffer = charset.decode(buffer.asByteBuffer());
            String body = charBuffer.toString();
            DataBufferUtils.release(buffer);
            MultiValueMap<String, String> formData = parseFormData(charset, body);
            logFormData(formData, hints);
            return formData;
        });
    }

    private void logFormData(MultiValueMap<String, String> formData, Map<String, Object> hints) {
        LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Read " + (isEnableLoggingRequestDetails() ? LogFormatUtils.formatValue(formData, !traceOn) : "form fields " + formData.keySet() + " (content masked)"));
    }

    private Charset getMediaTypeCharset(@Nullable MediaType mediaType) {
        if (mediaType != null && mediaType.getCharset() != null) {
            return mediaType.getCharset();
        } else {
            return getDefaultCharset();
        }
    }

    private MultiValueMap<String, String> parseFormData(Charset charset, String body) {
        String[] pairs = StringUtils.tokenizeToStringArray(body, "&");
        MultiValueMap<String, String> result = new LinkedMultiValueMap<>(pairs.length);
        try {
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
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
        return result;
    }

    @Override
    public List<MediaType> getReadableMediaTypes() {
        return Collections.singletonList(MediaType.APPLICATION_FORM_URLENCODED);
    }

}
