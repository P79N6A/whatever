package org.springframework.http.converter;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class ResourceRegionHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    public ResourceRegionHttpMessageConverter() {
        super(MediaType.ALL);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected MediaType getDefaultContentType(Object object) {
        Resource resource = null;
        if (object instanceof ResourceRegion) {
            resource = ((ResourceRegion) object).getResource();
        } else {
            Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
            if (!regions.isEmpty()) {
                resource = regions.iterator().next().getResource();
            }
        }
        return MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    public Object read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected ResourceRegion readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return canWrite(clazz, null, mediaType);
    }

    @Override
    public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
        if (!(type instanceof ParameterizedType)) {
            return (type instanceof Class && ResourceRegion.class.isAssignableFrom((Class<?>) type));
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (!(parameterizedType.getRawType() instanceof Class)) {
            return false;
        }
        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
        if (!(Collection.class.isAssignableFrom(rawType))) {
            return false;
        }
        if (parameterizedType.getActualTypeArguments().length != 1) {
            return false;
        }
        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        if (!(typeArgument instanceof Class)) {
            return false;
        }
        Class<?> typeArgumentClass = (Class<?>) typeArgument;
        return ResourceRegion.class.isAssignableFrom(typeArgumentClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void writeInternal(Object object, @Nullable Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        if (object instanceof ResourceRegion) {
            writeResourceRegion((ResourceRegion) object, outputMessage);
        } else {
            Collection<ResourceRegion> regions = (Collection<ResourceRegion>) object;
            if (regions.size() == 1) {
                writeResourceRegion(regions.iterator().next(), outputMessage);
            } else {
                writeResourceRegionCollection((Collection<ResourceRegion>) object, outputMessage);
            }
        }
    }

    protected void writeResourceRegion(ResourceRegion region, HttpOutputMessage outputMessage) throws IOException {
        Assert.notNull(region, "ResourceRegion must not be null");
        HttpHeaders responseHeaders = outputMessage.getHeaders();
        long start = region.getPosition();
        long end = start + region.getCount() - 1;
        Long resourceLength = region.getResource().contentLength();
        end = Math.min(end, resourceLength - 1);
        long rangeLength = end - start + 1;
        responseHeaders.add("Content-Range", "bytes " + start + '-' + end + '/' + resourceLength);
        responseHeaders.setContentLength(rangeLength);
        InputStream in = region.getResource().getInputStream();
        try {
            StreamUtils.copyRange(in, outputMessage.getBody(), start, end);
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                // ignore
            }
        }
    }

    private void writeResourceRegionCollection(Collection<ResourceRegion> resourceRegions, HttpOutputMessage outputMessage) throws IOException {
        Assert.notNull(resourceRegions, "Collection of ResourceRegion should not be null");
        HttpHeaders responseHeaders = outputMessage.getHeaders();
        MediaType contentType = responseHeaders.getContentType();
        String boundaryString = MimeTypeUtils.generateMultipartBoundaryString();
        responseHeaders.set(HttpHeaders.CONTENT_TYPE, "multipart/byteranges; boundary=" + boundaryString);
        OutputStream out = outputMessage.getBody();
        for (ResourceRegion region : resourceRegions) {
            long start = region.getPosition();
            long end = start + region.getCount() - 1;
            InputStream in = region.getResource().getInputStream();
            try {
                // Writing MIME header.
                println(out);
                print(out, "--" + boundaryString);
                println(out);
                if (contentType != null) {
                    print(out, "Content-Type: " + contentType.toString());
                    println(out);
                }
                Long resourceLength = region.getResource().contentLength();
                end = Math.min(end, resourceLength - 1);
                print(out, "Content-Range: bytes " + start + '-' + end + '/' + resourceLength);
                println(out);
                println(out);
                // Printing content
                StreamUtils.copyRange(in, out, start, end);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
        println(out);
        print(out, "--" + boundaryString + "--");
    }

    private static void println(OutputStream os) throws IOException {
        os.write('\r');
        os.write('\n');
    }

    private static void print(OutputStream os, String buf) throws IOException {
        os.write(buf.getBytes(StandardCharsets.US_ASCII));
    }

}
