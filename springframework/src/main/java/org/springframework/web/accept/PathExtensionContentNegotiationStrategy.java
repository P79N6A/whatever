package org.springframework.web.accept;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;

public class PathExtensionContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public PathExtensionContentNegotiationStrategy() {
        this(null);
    }

    public PathExtensionContentNegotiationStrategy(@Nullable Map<String, MediaType> mediaTypes) {
        super(mediaTypes);
        setUseRegisteredExtensionsOnly(false);
        setIgnoreUnknownExtensions(true);
        this.urlPathHelper.setUrlDecode(false);
    }

    public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
        this.urlPathHelper = urlPathHelper;
    }

    @Deprecated
    public void setUseJaf(boolean useJaf) {
        setUseRegisteredExtensionsOnly(!useJaf);
    }

    @Override
    @Nullable
    protected String getMediaTypeKey(NativeWebRequest webRequest) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return null;
        }
        // Ignore LOOKUP_PATH attribute, use our own "fixed" UrlPathHelper with decoding off
        String path = this.urlPathHelper.getLookupPathForRequest(request);
        String extension = UriUtils.extractFileExtension(path);
        return (StringUtils.hasText(extension) ? extension.toLowerCase(Locale.ENGLISH) : null);
    }

    @Nullable
    public MediaType getMediaTypeForResource(Resource resource) {
        Assert.notNull(resource, "Resource must not be null");
        MediaType mediaType = null;
        String filename = resource.getFilename();
        String extension = StringUtils.getFilenameExtension(filename);
        if (extension != null) {
            mediaType = lookupMediaType(extension);
        }
        if (mediaType == null) {
            mediaType = MediaTypeFactory.getMediaType(filename).orElse(null);
        }
        return mediaType;
    }

}
