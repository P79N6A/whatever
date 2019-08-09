package org.springframework.web.accept;

import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.Collections;
import java.util.List;

@FunctionalInterface
public interface ContentNegotiationStrategy {

    List<MediaType> MEDIA_TYPE_ALL_LIST = Collections.singletonList(MediaType.ALL);

    List<MediaType> resolveMediaTypes(NativeWebRequest webRequest) throws HttpMediaTypeNotAcceptableException;

}
