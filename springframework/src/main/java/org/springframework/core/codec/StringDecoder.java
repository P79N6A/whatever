package org.springframework.core.codec;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class StringDecoder extends AbstractDataBufferDecoder<String> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static final List<String> DEFAULT_DELIMITERS = Arrays.asList("\r\n", "\n");

    private final List<String> delimiters;

    private final boolean stripDelimiter;

    private final ConcurrentMap<Charset, byte[][]> delimitersCache = new ConcurrentHashMap<>();

    private StringDecoder(List<String> delimiters, boolean stripDelimiter, MimeType... mimeTypes) {
        super(mimeTypes);
        Assert.notEmpty(delimiters, "'delimiters' must not be empty");
        this.delimiters = new ArrayList<>(delimiters);
        this.stripDelimiter = stripDelimiter;
    }

    @Override
    public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
        return (elementType.resolve() == String.class && super.canDecode(elementType, mimeType));
    }

    @Override
    public Flux<String> decode(Publisher<DataBuffer> input, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        byte[][] delimiterBytes = getDelimiterBytes(mimeType);
        Flux<DataBuffer> inputFlux = DataBufferUtils.split(input, delimiterBytes, this.stripDelimiter);
        return super.decode(inputFlux, elementType, mimeType, hints);
    }

    private byte[][] getDelimiterBytes(@Nullable MimeType mimeType) {
        return this.delimitersCache.computeIfAbsent(getCharset(mimeType), charset -> {
            byte[][] result = new byte[this.delimiters.size()][];
            for (int i = 0; i < this.delimiters.size(); i++) {
                result[i] = this.delimiters.get(i).getBytes(charset);
            }
            return result;
        });
    }

    @Override
    public String decode(DataBuffer dataBuffer, ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
        Charset charset = getCharset(mimeType);
        CharBuffer charBuffer = charset.decode(dataBuffer.asByteBuffer());
        DataBufferUtils.release(dataBuffer);
        String value = charBuffer.toString();
        LogFormatUtils.traceDebug(logger, traceOn -> {
            String formatted = LogFormatUtils.formatValue(value, !traceOn);
            return Hints.getLogPrefix(hints) + "Decoded " + formatted;
        });
        return value;
    }

    private static Charset getCharset(@Nullable MimeType mimeType) {
        if (mimeType != null && mimeType.getCharset() != null) {
            return mimeType.getCharset();
        } else {
            return DEFAULT_CHARSET;
        }
    }

    @Deprecated
    public static StringDecoder textPlainOnly(boolean ignored) {
        return textPlainOnly();
    }

    public static StringDecoder textPlainOnly() {
        return textPlainOnly(DEFAULT_DELIMITERS, true);
    }

    public static StringDecoder textPlainOnly(List<String> delimiters, boolean stripDelimiter) {
        return new StringDecoder(delimiters, stripDelimiter, new MimeType("text", "plain", DEFAULT_CHARSET));
    }

    @Deprecated
    public static StringDecoder allMimeTypes(boolean ignored) {
        return allMimeTypes();
    }

    public static StringDecoder allMimeTypes() {
        return allMimeTypes(DEFAULT_DELIMITERS, true);
    }

    public static StringDecoder allMimeTypes(List<String> delimiters, boolean stripDelimiter) {
        return new StringDecoder(delimiters, stripDelimiter, new MimeType("text", "plain", DEFAULT_CHARSET), MimeTypeUtils.ALL);
    }

}
