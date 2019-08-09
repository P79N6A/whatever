package org.springframework.http.codec.support;

import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.*;
import org.springframework.http.codec.multipart.DefaultMultipartMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;

import java.util.List;

class ServerDefaultCodecsImpl extends BaseDefaultCodecs implements ServerCodecConfigurer.ServerDefaultCodecs {

    @Nullable
    private Encoder<?> sseEncoder;

    @Nullable
    private HttpMessageReader<Part> multipartReader;

    @Override
    public void serverSentEventEncoder(Encoder<?> encoder) {
        this.sseEncoder = encoder;
    }

    @Override
    public void multipartReader(HttpMessageReader<Part> multipartReader) {
        this.multipartReader = multipartReader;
    }

    @Override
    protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) {
        HttpMessageReader<Part> partReader = getMultipartReader();
        boolean logRequestDetails = isEnableLoggingRequestDetails();
        if (partReader instanceof LoggingCodecSupport) {
            ((LoggingCodecSupport) partReader).setEnableLoggingRequestDetails(logRequestDetails);
        }
        typedReaders.add(partReader);
        MultipartHttpMessageReader reader = new MultipartHttpMessageReader(partReader);
        reader.setEnableLoggingRequestDetails(logRequestDetails);
        typedReaders.add(reader);
    }

    private HttpMessageReader<Part> getMultipartReader() {
        return this.multipartReader != null ? this.multipartReader : new DefaultMultipartMessageReader();
    }

    @Override
    protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
        objectWriters.add(new ServerSentEventHttpMessageWriter(getSseEncoder()));
    }

    @Nullable
    private Encoder<?> getSseEncoder() {
        return this.sseEncoder != null ? this.sseEncoder : jackson2Present ? getJackson2JsonEncoder() : null;
    }

}
