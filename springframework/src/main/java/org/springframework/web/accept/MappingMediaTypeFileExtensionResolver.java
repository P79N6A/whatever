package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MappingMediaTypeFileExtensionResolver implements MediaTypeFileExtensionResolver {

    private final ConcurrentMap<String, MediaType> mediaTypes = new ConcurrentHashMap<>(64);

    private final MultiValueMap<MediaType, String> fileExtensions = new LinkedMultiValueMap<>();

    private final List<String> allFileExtensions = new ArrayList<>();

    public MappingMediaTypeFileExtensionResolver(@Nullable Map<String, MediaType> mediaTypes) {
        if (mediaTypes != null) {
            mediaTypes.forEach((extension, mediaType) -> {
                String lowerCaseExtension = extension.toLowerCase(Locale.ENGLISH);
                this.mediaTypes.put(lowerCaseExtension, mediaType);
                this.fileExtensions.add(mediaType, lowerCaseExtension);
                this.allFileExtensions.add(lowerCaseExtension);
            });
        }
    }

    public Map<String, MediaType> getMediaTypes() {
        return this.mediaTypes;
    }

    protected List<MediaType> getAllMediaTypes() {
        return new ArrayList<>(this.mediaTypes.values());
    }

    protected void addMapping(String extension, MediaType mediaType) {
        MediaType previous = this.mediaTypes.putIfAbsent(extension, mediaType);
        if (previous == null) {
            this.fileExtensions.add(mediaType, extension);
            this.allFileExtensions.add(extension);
        }
    }

    @Override
    public List<String> resolveFileExtensions(MediaType mediaType) {
        List<String> fileExtensions = this.fileExtensions.get(mediaType);
        return (fileExtensions != null ? fileExtensions : Collections.emptyList());
    }

    @Override
    public List<String> getAllFileExtensions() {
        return Collections.unmodifiableList(this.allFileExtensions);
    }

    @Nullable
    protected MediaType lookupMediaType(String extension) {
        return this.mediaTypes.get(extension.toLowerCase(Locale.ENGLISH));
    }

}
