package org.springframework.http.converter;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class BufferedImageHttpMessageConverter implements HttpMessageConverter<BufferedImage> {

    private final List<MediaType> readableMediaTypes = new ArrayList<>();

    @Nullable
    private MediaType defaultContentType;

    @Nullable
    private File cacheDir;

    public BufferedImageHttpMessageConverter() {
        String[] readerMediaTypes = ImageIO.getReaderMIMETypes();
        for (String mediaType : readerMediaTypes) {
            if (StringUtils.hasText(mediaType)) {
                this.readableMediaTypes.add(MediaType.parseMediaType(mediaType));
            }
        }
        String[] writerMediaTypes = ImageIO.getWriterMIMETypes();
        for (String mediaType : writerMediaTypes) {
            if (StringUtils.hasText(mediaType)) {
                this.defaultContentType = MediaType.parseMediaType(mediaType);
                break;
            }
        }
    }

    public void setDefaultContentType(@Nullable MediaType defaultContentType) {
        if (defaultContentType != null) {
            Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(defaultContentType.toString());
            if (!imageWriters.hasNext()) {
                throw new IllegalArgumentException("Content-Type [" + defaultContentType + "] is not supported by the Java Image I/O API");
            }
        }
        this.defaultContentType = defaultContentType;
    }

    @Nullable
    public MediaType getDefaultContentType() {
        return this.defaultContentType;
    }

    public void setCacheDir(File cacheDir) {
        Assert.notNull(cacheDir, "'cacheDir' must not be null");
        Assert.isTrue(cacheDir.isDirectory(), "'cacheDir' is not a directory");
        this.cacheDir = cacheDir;
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return (BufferedImage.class == clazz && isReadable(mediaType));
    }

    private boolean isReadable(@Nullable MediaType mediaType) {
        if (mediaType == null) {
            return true;
        }
        Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(mediaType.toString());
        return imageReaders.hasNext();
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return (BufferedImage.class == clazz && isWritable(mediaType));
    }

    private boolean isWritable(@Nullable MediaType mediaType) {
        if (mediaType == null || MediaType.ALL.equalsTypeAndSubtype(mediaType)) {
            return true;
        }
        Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(mediaType.toString());
        return imageWriters.hasNext();
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.unmodifiableList(this.readableMediaTypes);
    }

    @Override
    public BufferedImage read(@Nullable Class<? extends BufferedImage> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        ImageInputStream imageInputStream = null;
        ImageReader imageReader = null;
        try {
            imageInputStream = createImageInputStream(inputMessage.getBody());
            MediaType contentType = inputMessage.getHeaders().getContentType();
            if (contentType == null) {
                throw new HttpMessageNotReadableException("No Content-Type header", inputMessage);
            }
            Iterator<ImageReader> imageReaders = ImageIO.getImageReadersByMIMEType(contentType.toString());
            if (imageReaders.hasNext()) {
                imageReader = imageReaders.next();
                ImageReadParam irp = imageReader.getDefaultReadParam();
                process(irp);
                imageReader.setInput(imageInputStream, true);
                return imageReader.read(0, irp);
            } else {
                throw new HttpMessageNotReadableException("Could not find javax.imageio.ImageReader for Content-Type [" + contentType + "]", inputMessage);
            }
        } finally {
            if (imageReader != null) {
                imageReader.dispose();
            }
            if (imageInputStream != null) {
                try {
                    imageInputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    private ImageInputStream createImageInputStream(InputStream is) throws IOException {
        if (this.cacheDir != null) {
            return new FileCacheImageInputStream(is, this.cacheDir);
        } else {
            return new MemoryCacheImageInputStream(is);
        }
    }

    @Override
    public void write(final BufferedImage image, @Nullable final MediaType contentType, final HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        final MediaType selectedContentType = getContentType(contentType);
        outputMessage.getHeaders().setContentType(selectedContentType);
        if (outputMessage instanceof StreamingHttpOutputMessage) {
            StreamingHttpOutputMessage streamingOutputMessage = (StreamingHttpOutputMessage) outputMessage;
            streamingOutputMessage.setBody(outputStream -> writeInternal(image, selectedContentType, outputStream));
        } else {
            writeInternal(image, selectedContentType, outputMessage.getBody());
        }
    }

    private MediaType getContentType(@Nullable MediaType contentType) {
        if (contentType == null || contentType.isWildcardType() || contentType.isWildcardSubtype()) {
            contentType = getDefaultContentType();
        }
        Assert.notNull(contentType, "Could not select Content-Type. " + "Please specify one through the 'defaultContentType' property.");
        return contentType;
    }

    private void writeInternal(BufferedImage image, MediaType contentType, OutputStream body) throws IOException, HttpMessageNotWritableException {
        ImageOutputStream imageOutputStream = null;
        ImageWriter imageWriter = null;
        try {
            Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByMIMEType(contentType.toString());
            if (imageWriters.hasNext()) {
                imageWriter = imageWriters.next();
                ImageWriteParam iwp = imageWriter.getDefaultWriteParam();
                process(iwp);
                imageOutputStream = createImageOutputStream(body);
                imageWriter.setOutput(imageOutputStream);
                imageWriter.write(null, new IIOImage(image, null, null), iwp);
            } else {
                throw new HttpMessageNotWritableException("Could not find javax.imageio.ImageWriter for Content-Type [" + contentType + "]");
            }
        } finally {
            if (imageWriter != null) {
                imageWriter.dispose();
            }
            if (imageOutputStream != null) {
                try {
                    imageOutputStream.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    private ImageOutputStream createImageOutputStream(OutputStream os) throws IOException {
        if (this.cacheDir != null) {
            return new FileCacheImageOutputStream(os, this.cacheDir);
        } else {
            return new MemoryCacheImageOutputStream(os);
        }
    }

    protected void process(ImageReadParam irp) {
    }

    protected void process(ImageWriteParam iwp) {
    }

}
