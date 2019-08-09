package org.springframework.web.multipart;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;

import javax.servlet.http.HttpServletRequest;

public interface MultipartHttpServletRequest extends HttpServletRequest, MultipartRequest {

    @Nullable
    HttpMethod getRequestMethod();

    HttpHeaders getRequestHeaders();

    @Nullable
    HttpHeaders getMultipartHeaders(String paramOrFileName);

}
