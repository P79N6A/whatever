package org.springframework.web.accept;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import javax.servlet.ServletContext;
import java.util.Map;

public class ServletPathExtensionContentNegotiationStrategy extends PathExtensionContentNegotiationStrategy {

    private final ServletContext servletContext;

    public ServletPathExtensionContentNegotiationStrategy(ServletContext context) {
        this(context, null);
    }

    public ServletPathExtensionContentNegotiationStrategy(ServletContext servletContext, @Nullable Map<String, MediaType> mediaTypes) {
        super(mediaTypes);
        Assert.notNull(servletContext, "ServletContext is required");
        this.servletContext = servletContext;
    }

    @Override
    @Nullable
    protected MediaType handleNoMatch(NativeWebRequest webRequest, String extension) throws HttpMediaTypeNotAcceptableException {
        MediaType mediaType = null;
        String mimeType = this.servletContext.getMimeType("file." + extension);
        if (StringUtils.hasText(mimeType)) {
            mediaType = MediaType.parseMediaType(mimeType);
        }
        if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
            MediaType superMediaType = super.handleNoMatch(webRequest, extension);
            if (superMediaType != null) {
                mediaType = superMediaType;
            }
        }
        return mediaType;
    }

    @Override
    public MediaType getMediaTypeForResource(Resource resource) {
        MediaType mediaType = null;
        String mimeType = this.servletContext.getMimeType(resource.getFilename());
        if (StringUtils.hasText(mimeType)) {
            mediaType = MediaType.parseMediaType(mimeType);
        }
        if (mediaType == null || MediaType.APPLICATION_OCTET_STREAM.equals(mediaType)) {
            MediaType superMediaType = super.getMediaTypeForResource(resource);
            if (superMediaType != null) {
                mediaType = superMediaType;
            }
        }
        return mediaType;
    }

}
