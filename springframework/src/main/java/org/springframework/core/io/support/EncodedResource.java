package org.springframework.core.io.support;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

public class EncodedResource implements InputStreamSource {

    private final Resource resource;

    @Nullable
    private final String encoding;

    @Nullable
    private final Charset charset;

    public EncodedResource(Resource resource) {
        this(resource, null, null);
    }

    public EncodedResource(Resource resource, @Nullable String encoding) {
        this(resource, encoding, null);
    }

    public EncodedResource(Resource resource, @Nullable Charset charset) {
        this(resource, null, charset);
    }

    private EncodedResource(Resource resource, @Nullable String encoding, @Nullable Charset charset) {
        super();
        Assert.notNull(resource, "Resource must not be null");
        this.resource = resource;
        this.encoding = encoding;
        this.charset = charset;
    }

    public final Resource getResource() {
        return this.resource;
    }

    @Nullable
    public final String getEncoding() {
        return this.encoding;
    }

    @Nullable
    public final Charset getCharset() {
        return this.charset;
    }

    public boolean requiresReader() {
        return (this.encoding != null || this.charset != null);
    }

    public Reader getReader() throws IOException {
        if (this.charset != null) {
            return new InputStreamReader(this.resource.getInputStream(), this.charset);
        } else if (this.encoding != null) {
            return new InputStreamReader(this.resource.getInputStream(), this.encoding);
        } else {
            return new InputStreamReader(this.resource.getInputStream());
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return this.resource.getInputStream();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EncodedResource)) {
            return false;
        }
        EncodedResource otherResource = (EncodedResource) other;
        return (this.resource.equals(otherResource.resource) && ObjectUtils.nullSafeEquals(this.charset, otherResource.charset) && ObjectUtils.nullSafeEquals(this.encoding, otherResource.encoding));
    }

    @Override
    public int hashCode() {
        return this.resource.hashCode();
    }

    @Override
    public String toString() {
        return this.resource.toString();
    }

}
