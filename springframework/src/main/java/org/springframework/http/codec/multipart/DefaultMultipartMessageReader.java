package org.springframework.http.codec.multipart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.*;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.Channel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultMultipartMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part> {

    private static final Log logger = LogFactory.getLog(DefaultMultipartMessageReader.class);

    private static final byte CR = '\r';

    private static final byte LF = '\n';

    private static final byte HYPHEN = '-';

    private static final byte[] FIRST_BOUNDARY_PREFIX = {HYPHEN, HYPHEN};

    private static final byte[] BOUNDARY_PREFIX = {CR, LF, HYPHEN, HYPHEN};

    private static final byte[] HEADER_BODY_SEPARATOR = {CR, LF, CR, LF};

    private static final String HEADER_SEPARATOR = "\\r\\n";

    private static final DataBufferUtils.Matcher HEADER_MATCHER = DataBufferUtils.matcher(HEADER_BODY_SEPARATOR);

    @Override
    public List<MediaType> getReadableMediaTypes() {
        return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
    }

    @Override
    public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
        return (Part.class.equals(elementType.toClass()) && (mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)));
    }

    @Override
    public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        byte[] boundary = boundary(message);
        if (boundary == null) {
            return Flux.error(new CodecException("No multipart boundary found in Content-Type: \"" + message.getHeaders().getContentType() + "\""));
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Boundary: " + toString(boundary));
        }
        byte[] boundaryNeedle = concat(BOUNDARY_PREFIX, boundary);
        Flux<DataBuffer> body = skipUntilFirstBoundary(message.getBody(), boundary);
        return DataBufferUtils.split(body, boundaryNeedle).takeWhile(DefaultMultipartMessageReader::notLastBoundary).map(DefaultMultipartMessageReader::toPart).doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release).doOnDiscard(DefaultPart.class, part -> DataBufferUtils.release(part.body));
    }

    @Nullable
    private static byte[] boundary(HttpMessage message) {
        MediaType contentType = message.getHeaders().getContentType();
        if (contentType != null) {
            String boundary = contentType.getParameter("boundary");
            if (boundary != null) {
                return boundary.getBytes(StandardCharsets.ISO_8859_1);
            }
        }
        return null;
    }

    private static Flux<DataBuffer> skipUntilFirstBoundary(Flux<DataBuffer> dataBuffers, byte[] boundary) {
        byte[] needle = concat(FIRST_BOUNDARY_PREFIX, boundary);
        DataBufferUtils.Matcher matcher = DataBufferUtils.matcher(needle);
        AtomicBoolean found = new AtomicBoolean();
        return dataBuffers.concatMap(dataBuffer -> {
            if (found.get()) {
                return Mono.just(dataBuffer);
            } else {
                int endIdx = matcher.match(dataBuffer);
                if (endIdx != -1) {
                    found.set(true);
                    int length = dataBuffer.writePosition() - 1 - endIdx;
                    DataBuffer slice = dataBuffer.retainedSlice(endIdx + 1, length);
                    DataBufferUtils.release(dataBuffer);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found first boundary at " + endIdx + " in " + toString(dataBuffer));
                    }
                    return Mono.just(slice);
                } else {
                    DataBufferUtils.release(dataBuffer);
                    return Mono.empty();
                }
            }
        });
    }

    private static boolean notLastBoundary(DataBuffer dataBuffer) {
        if (dataBuffer.readableByteCount() >= 2) {
            int readPosition = dataBuffer.readPosition();
            if (dataBuffer.getByte(readPosition) == HYPHEN && dataBuffer.getByte(readPosition + 1) == HYPHEN) {
                DataBufferUtils.release(dataBuffer);
                return false;
            }
        }
        return true;
    }

    private static Part toPart(DataBuffer dataBuffer) {
        int readPosition = dataBuffer.readPosition();
        if (dataBuffer.readableByteCount() >= 2) {
            if (dataBuffer.getByte(readPosition) == CR && dataBuffer.getByte(readPosition + 1) == LF) {
                dataBuffer.readPosition(readPosition + 2);
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("Part data: " + toString(dataBuffer));
        }
        int endIdx = HEADER_MATCHER.match(dataBuffer);
        HttpHeaders headers;
        DataBuffer body;
        if (endIdx > 0) {
            readPosition = dataBuffer.readPosition();
            int headersLength = endIdx + 1 - (readPosition + HEADER_BODY_SEPARATOR.length);
            DataBuffer headersBuffer = dataBuffer.retainedSlice(readPosition, headersLength);
            int bodyLength = dataBuffer.writePosition() - (1 + endIdx);
            body = dataBuffer.retainedSlice(endIdx + 1, bodyLength);
            headers = toHeaders(headersBuffer);
        } else {
            headers = new HttpHeaders();
            body = DataBufferUtils.retain(dataBuffer);
        }
        DataBufferUtils.release(dataBuffer);
        ContentDisposition cd = headers.getContentDisposition();
        MediaType contentType = headers.getContentType();
        if (StringUtils.hasLength(cd.getFilename())) {
            return new DefaultFilePart(headers, body);
        } else if (StringUtils.hasLength(cd.getName()) && (contentType == null || MediaType.TEXT_PLAIN.isCompatibleWith(contentType))) {
            return new DefaultFormPart(headers, body);
        } else {
            return new DefaultPart(headers, body);
        }
    }

    private static HttpHeaders toHeaders(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(bytes);
        DataBufferUtils.release(dataBuffer);
        String string = new String(bytes, StandardCharsets.US_ASCII);
        String[] lines = string.split(HEADER_SEPARATOR);
        HttpHeaders result = new HttpHeaders();
        for (String line : lines) {
            int idx = line.indexOf(':');
            if (idx != -1) {
                String name = line.substring(0, idx);
                String value = line.substring(idx + 1);
                while (value.startsWith(" ")) {
                    value = value.substring(1);
                }
                String[] tokens = StringUtils.tokenizeToStringArray(value, ",");
                for (String token : tokens) {
                    result.add(name, token);
                }
            }
        }
        return result;
    }

    private static String toString(DataBuffer dataBuffer) {
        byte[] bytes = new byte[dataBuffer.readableByteCount()];
        int j = 0;
        for (int i = dataBuffer.readPosition(); i < dataBuffer.writePosition(); i++) {
            bytes[j++] = dataBuffer.getByte(i);
        }
        return toString(bytes);
    }

    private static String toString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            if (b == CR) {
                builder.append("␍");
            } else if (b == LF) {
                builder.append("␤");
            } else if (b >= 20 && b <= 126) {
                builder.append((char) b);
            }
        }
        return builder.toString();
    }

    @Override
    public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
        return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
    }

    private static byte[] concat(byte[]... byteArrays) {
        int length = 0;
        for (byte[] byteArray : byteArrays) {
            length += byteArray.length;
        }
        byte[] result = new byte[length];
        length = 0;
        for (byte[] byteArray : byteArrays) {
            System.arraycopy(byteArray, 0, result, length, byteArray.length);
            length += byteArray.length;
        }
        return result;
    }

    private static class DefaultPart implements Part {

        private final HttpHeaders headers;

        protected final DataBuffer body;

        public DefaultPart(HttpHeaders headers, DataBuffer body) {
            this.headers = headers;
            this.body = body;
        }

        @Override
        public String name() {
            String name = headers().getContentDisposition().getName();
            Assert.state(name != null, "No name available");
            return name;
        }

        @Override
        public HttpHeaders headers() {
            return this.headers;
        }

        @Override
        public Flux<DataBuffer> content() {
            return Flux.just(this.body);
        }

    }

    private static class DefaultFormPart extends DefaultPart implements FormFieldPart {

        private String value;

        public DefaultFormPart(HttpHeaders headers, DataBuffer body) {
            super(headers, body);
            this.value = toString(body, contentTypeCharset(headers));
        }

        private static String toString(DataBuffer dataBuffer, Charset charset) {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            return new String(bytes, charset).trim();
        }

        private static Charset contentTypeCharset(HttpHeaders headers) {
            MediaType contentType = headers.getContentType();
            if (contentType != null) {
                Charset charset = contentType.getCharset();
                if (charset != null) {
                    return charset;
                }
            }
            return StandardCharsets.ISO_8859_1;
        }

        @Override
        public String value() {
            return this.value;
        }

    }

    private static class DefaultFilePart extends DefaultPart implements FilePart {

        public DefaultFilePart(HttpHeaders headers, DataBuffer body) {
            super(headers, body);
        }

        @Override
        public String filename() {
            String filename = headers().getContentDisposition().getFilename();
            Assert.state(filename != null, "No filename available");
            return filename;
        }

        @Override
        public Mono<Void> transferTo(Path dest) {
            return Mono.using(() -> AsynchronousFileChannel.open(dest, StandardOpenOption.WRITE), this::writeBody, this::close);
        }

        private Mono<Void> writeBody(AsynchronousFileChannel channel) {
            return DataBufferUtils.write(content(), channel).map(DataBufferUtils::release).then();
        }

        private void close(Channel channel) {
            try {
                channel.close();
            } catch (IOException ignore) {
            }
        }

    }

}
