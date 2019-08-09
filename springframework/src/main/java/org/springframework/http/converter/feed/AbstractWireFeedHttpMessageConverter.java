package org.springframework.http.converter.feed;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.WireFeedInput;
import com.rometools.rome.io.WireFeedOutput;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class AbstractWireFeedHttpMessageConverter<T extends WireFeed> extends AbstractHttpMessageConverter<T> {

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    protected AbstractWireFeedHttpMessageConverter(MediaType supportedMediaType) {
        super(supportedMediaType);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        WireFeedInput feedInput = new WireFeedInput();
        MediaType contentType = inputMessage.getHeaders().getContentType();
        Charset charset = (contentType != null && contentType.getCharset() != null ? contentType.getCharset() : DEFAULT_CHARSET);
        try {
            Reader reader = new InputStreamReader(inputMessage.getBody(), charset);
            return (T) feedInput.build(reader);
        } catch (FeedException ex) {
            throw new HttpMessageNotReadableException("Could not read WireFeed: " + ex.getMessage(), ex, inputMessage);
        }
    }

    @Override
    protected void writeInternal(T wireFeed, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        Charset charset = (StringUtils.hasLength(wireFeed.getEncoding()) ? Charset.forName(wireFeed.getEncoding()) : DEFAULT_CHARSET);
        MediaType contentType = outputMessage.getHeaders().getContentType();
        if (contentType != null) {
            contentType = new MediaType(contentType.getType(), contentType.getSubtype(), charset);
            outputMessage.getHeaders().setContentType(contentType);
        }
        WireFeedOutput feedOutput = new WireFeedOutput();
        try {
            Writer writer = new OutputStreamWriter(outputMessage.getBody(), charset);
            feedOutput.output(wireFeed, writer);
        } catch (FeedException ex) {
            throw new HttpMessageNotWritableException("Could not write WireFeed: " + ex.getMessage(), ex);
        }
    }

}
