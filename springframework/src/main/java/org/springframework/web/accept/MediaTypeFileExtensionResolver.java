package org.springframework.web.accept;

import org.springframework.http.MediaType;

import java.util.List;

public interface MediaTypeFileExtensionResolver {

    List<String> resolveFileExtensions(MediaType mediaType);

    List<String> getAllFileExtensions();

}
