package org.springframework.http.converter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StringHttpMessageConverter extends AbstractHttpMessageConverter<String> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    @Nullable
    private volatile List<Charset> availableCharsets;

    private boolean writeAcceptCharset = false;

    public StringHttpMessageConverter() {
        this(DEFAULT_CHARSET);
    }

    public StringHttpMessageConverter(Charset defaultCharset) {
        super(defaultCharset, MediaType.TEXT_PLAIN, MediaType.ALL);
    }

    public void setWriteAcceptCharset(boolean writeAcceptCharset) {
        this.writeAcceptCharset = writeAcceptCharset;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return String.class == clazz;
    }

    @Override
    protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage) throws IOException {
        Charset charset = getContentTypeCharset(inputMessage.getHeaders().getContentType());
        return StreamUtils.copyToString(inputMessage.getBody(), charset);
    }

    @Override
    protected Long getContentLength(String str, @Nullable MediaType contentType) {
        Charset charset = getContentTypeCharset(contentType);
        return (long) str.getBytes(charset).length;
    }

    @Override
    protected void writeInternal(String str, HttpOutputMessage outputMessage) throws IOException {
        HttpHeaders headers = outputMessage.getHeaders();
        if (this.writeAcceptCharset && headers.get(HttpHeaders.ACCEPT_CHARSET) == null) {
            headers.setAcceptCharset(getAcceptedCharsets());
        }
        Charset charset = getContentTypeCharset(headers.getContentType());
        StreamUtils.copy(str, charset, outputMessage.getBody());
    }

    protected List<Charset> getAcceptedCharsets() {
        List<Charset> charsets = this.availableCharsets;
        if (charsets == null) {
            charsets = new ArrayList<>(Charset.availableCharsets().values());
            this.availableCharsets = charsets;
        }
        return charsets;
    }

    private Charset getContentTypeCharset(@Nullable MediaType contentType) {
        if (contentType != null && contentType.getCharset() != null) {
            return contentType.getCharset();
        } else if (contentType != null && contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            // Matching to AbstractJackson2HttpMessageConverter#DEFAULT_CHARSET
            return StandardCharsets.UTF_8;
        } else {
            Charset charset = getDefaultCharset();
            Assert.state(charset != null, "No default charset");
            return charset;
        }
    }

}
