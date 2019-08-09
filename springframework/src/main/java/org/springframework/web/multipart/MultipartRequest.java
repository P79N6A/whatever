package org.springframework.web.multipart;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface MultipartRequest {

    Iterator<String> getFileNames();

    @Nullable
    MultipartFile getFile(String name);

    List<MultipartFile> getFiles(String name);

    Map<String, MultipartFile> getFileMap();

    MultiValueMap<String, MultipartFile> getMultiFileMap();

    @Nullable
    String getMultipartContentType(String paramOrFileName);

}
