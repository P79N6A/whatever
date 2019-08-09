package org.springframework.web.multipart.support;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public abstract class AbstractMultipartHttpServletRequest extends HttpServletRequestWrapper implements MultipartHttpServletRequest {

    @Nullable
    private MultiValueMap<String, MultipartFile> multipartFiles;

    protected AbstractMultipartHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public HttpServletRequest getRequest() {
        return (HttpServletRequest) super.getRequest();
    }

    @Override
    public HttpMethod getRequestMethod() {
        return HttpMethod.resolve(getRequest().getMethod());
    }

    @Override
    public HttpHeaders getRequestHeaders() {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, Collections.list(getHeaders(headerName)));
        }
        return headers;
    }

    @Override
    public Iterator<String> getFileNames() {
        return getMultipartFiles().keySet().iterator();
    }

    @Override
    public MultipartFile getFile(String name) {
        return getMultipartFiles().getFirst(name);
    }

    @Override
    public List<MultipartFile> getFiles(String name) {
        List<MultipartFile> multipartFiles = getMultipartFiles().get(name);
        if (multipartFiles != null) {
            return multipartFiles;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, MultipartFile> getFileMap() {
        return getMultipartFiles().toSingleValueMap();
    }

    @Override
    public MultiValueMap<String, MultipartFile> getMultiFileMap() {
        return getMultipartFiles();
    }

    public boolean isResolved() {
        return (this.multipartFiles != null);
    }

    protected final void setMultipartFiles(MultiValueMap<String, MultipartFile> multipartFiles) {
        this.multipartFiles = new LinkedMultiValueMap<>(Collections.unmodifiableMap(multipartFiles));
    }

    protected MultiValueMap<String, MultipartFile> getMultipartFiles() {
        if (this.multipartFiles == null) {
            initializeMultipart();
        }
        return this.multipartFiles;
    }

    protected void initializeMultipart() {
        throw new IllegalStateException("Multipart request not initialized");
    }

}
