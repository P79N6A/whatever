package org.springframework.http.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ByteArrayHttpMessageConverter extends AbstractHttpMessageConverter<byte[]> {

    public ByteArrayHttpMessageConverter() {
        super(MediaType.APPLICATION_OCTET_STREAM, MediaType.ALL);
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return byte[].class == clazz;
    }

    @Override
    public byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage inputMessage) throws IOException {
        long contentLength = inputMessage.getHeaders().getContentLength();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(contentLength >= 0 ? (int) contentLength : StreamUtils.BUFFER_SIZE);
        StreamUtils.copy(inputMessage.getBody(), bos);
        return bos.toByteArray();
    }

    @Override
    protected Long getContentLength(byte[] bytes, @Nullable MediaType contentType) {
        return (long) bytes.length;
    }

    @Override
    protected void writeInternal(byte[] bytes, HttpOutputMessage outputMessage) throws IOException {
        StreamUtils.copy(bytes, outputMessage.getBody());
    }

}
