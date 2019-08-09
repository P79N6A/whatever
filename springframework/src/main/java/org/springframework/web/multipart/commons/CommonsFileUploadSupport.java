package org.springframework.web.multipart.commons;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CommonsFileUploadSupport {

    protected final Log logger = LogFactory.getLog(getClass());

    private final DiskFileItemFactory fileItemFactory;

    private final FileUpload fileUpload;

    private boolean uploadTempDirSpecified = false;

    private boolean preserveFilename = false;

    public CommonsFileUploadSupport() {
        this.fileItemFactory = newFileItemFactory();
        this.fileUpload = newFileUpload(getFileItemFactory());
    }

    public DiskFileItemFactory getFileItemFactory() {
        return this.fileItemFactory;
    }

    public FileUpload getFileUpload() {
        return this.fileUpload;
    }

    public void setMaxUploadSize(long maxUploadSize) {
        this.fileUpload.setSizeMax(maxUploadSize);
    }

    public void setMaxUploadSizePerFile(long maxUploadSizePerFile) {
        this.fileUpload.setFileSizeMax(maxUploadSizePerFile);
    }

    public void setMaxInMemorySize(int maxInMemorySize) {
        this.fileItemFactory.setSizeThreshold(maxInMemorySize);
    }

    public void setDefaultEncoding(String defaultEncoding) {
        this.fileUpload.setHeaderEncoding(defaultEncoding);
    }

    protected String getDefaultEncoding() {
        String encoding = getFileUpload().getHeaderEncoding();
        if (encoding == null) {
            encoding = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
        return encoding;
    }

    public void setUploadTempDir(Resource uploadTempDir) throws IOException {
        if (!uploadTempDir.exists() && !uploadTempDir.getFile().mkdirs()) {
            throw new IllegalArgumentException("Given uploadTempDir [" + uploadTempDir + "] could not be created");
        }
        this.fileItemFactory.setRepository(uploadTempDir.getFile());
        this.uploadTempDirSpecified = true;
    }

    protected boolean isUploadTempDirSpecified() {
        return this.uploadTempDirSpecified;
    }

    public void setPreserveFilename(boolean preserveFilename) {
        this.preserveFilename = preserveFilename;
    }

    protected DiskFileItemFactory newFileItemFactory() {
        return new DiskFileItemFactory();
    }

    protected abstract FileUpload newFileUpload(FileItemFactory fileItemFactory);

    protected FileUpload prepareFileUpload(@Nullable String encoding) {
        FileUpload fileUpload = getFileUpload();
        FileUpload actualFileUpload = fileUpload;
        // Use new temporary FileUpload instance if the request specifies
        // its own encoding that does not match the default encoding.
        if (encoding != null && !encoding.equals(fileUpload.getHeaderEncoding())) {
            actualFileUpload = newFileUpload(getFileItemFactory());
            actualFileUpload.setSizeMax(fileUpload.getSizeMax());
            actualFileUpload.setFileSizeMax(fileUpload.getFileSizeMax());
            actualFileUpload.setHeaderEncoding(encoding);
        }
        return actualFileUpload;
    }

    protected MultipartParsingResult parseFileItems(List<FileItem> fileItems, String encoding) {
        MultiValueMap<String, MultipartFile> multipartFiles = new LinkedMultiValueMap<>();
        Map<String, String[]> multipartParameters = new HashMap<>();
        Map<String, String> multipartParameterContentTypes = new HashMap<>();
        // Extract multipart files and multipart parameters.
        for (FileItem fileItem : fileItems) {
            if (fileItem.isFormField()) {
                String value;
                String partEncoding = determineEncoding(fileItem.getContentType(), encoding);
                try {
                    value = fileItem.getString(partEncoding);
                } catch (UnsupportedEncodingException ex) {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Could not decode multipart item '" + fileItem.getFieldName() + "' with encoding '" + partEncoding + "': using platform default");
                    }
                    value = fileItem.getString();
                }
                String[] curParam = multipartParameters.get(fileItem.getFieldName());
                if (curParam == null) {
                    // simple form field
                    multipartParameters.put(fileItem.getFieldName(), new String[]{value});
                } else {
                    // array of simple form fields
                    String[] newParam = StringUtils.addStringToArray(curParam, value);
                    multipartParameters.put(fileItem.getFieldName(), newParam);
                }
                multipartParameterContentTypes.put(fileItem.getFieldName(), fileItem.getContentType());
            } else {
                // multipart file field
                CommonsMultipartFile file = createMultipartFile(fileItem);
                multipartFiles.add(file.getName(), file);
                LogFormatUtils.traceDebug(logger, traceOn -> "Part '" + file.getName() + "', size " + file.getSize() + " bytes, filename='" + file.getOriginalFilename() + "'" + (traceOn ? ", storage=" + file.getStorageDescription() : ""));
            }
        }
        return new MultipartParsingResult(multipartFiles, multipartParameters, multipartParameterContentTypes);
    }

    protected CommonsMultipartFile createMultipartFile(FileItem fileItem) {
        CommonsMultipartFile multipartFile = new CommonsMultipartFile(fileItem);
        multipartFile.setPreserveFilename(this.preserveFilename);
        return multipartFile;
    }

    protected void cleanupFileItems(MultiValueMap<String, MultipartFile> multipartFiles) {
        for (List<MultipartFile> files : multipartFiles.values()) {
            for (MultipartFile file : files) {
                if (file instanceof CommonsMultipartFile) {
                    CommonsMultipartFile cmf = (CommonsMultipartFile) file;
                    cmf.getFileItem().delete();
                    LogFormatUtils.traceDebug(logger, traceOn -> "Cleaning up part '" + cmf.getName() + "', filename '" + cmf.getOriginalFilename() + "'" + (traceOn ? ", stored " + cmf.getStorageDescription() : ""));
                }
            }
        }
    }

    private String determineEncoding(String contentTypeHeader, String defaultEncoding) {
        if (!StringUtils.hasText(contentTypeHeader)) {
            return defaultEncoding;
        }
        MediaType contentType = MediaType.parseMediaType(contentTypeHeader);
        Charset charset = contentType.getCharset();
        return (charset != null ? charset.name() : defaultEncoding);
    }

    protected static class MultipartParsingResult {

        private final MultiValueMap<String, MultipartFile> multipartFiles;

        private final Map<String, String[]> multipartParameters;

        private final Map<String, String> multipartParameterContentTypes;

        public MultipartParsingResult(MultiValueMap<String, MultipartFile> mpFiles, Map<String, String[]> mpParams, Map<String, String> mpParamContentTypes) {
            this.multipartFiles = mpFiles;
            this.multipartParameters = mpParams;
            this.multipartParameterContentTypes = mpParamContentTypes;
        }

        public MultiValueMap<String, MultipartFile> getMultipartFiles() {
            return this.multipartFiles;
        }

        public Map<String, String[]> getMultipartParameters() {
            return this.multipartParameters;
        }

        public Map<String, String> getMultipartParameterContentTypes() {
            return this.multipartParameterContentTypes;
        }

    }

}
