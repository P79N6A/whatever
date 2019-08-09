package org.springframework.web.multipart.commons;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.springframework.util.Assert;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.AbstractMultipartHttpServletRequest;
import org.springframework.web.multipart.support.DefaultMultipartHttpServletRequest;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class CommonsMultipartResolver extends CommonsFileUploadSupport implements MultipartResolver, ServletContextAware {

    private boolean resolveLazily = false;

    public CommonsMultipartResolver() {
        super();
    }

    public CommonsMultipartResolver(ServletContext servletContext) {
        this();
        setServletContext(servletContext);
    }

    public void setResolveLazily(boolean resolveLazily) {
        this.resolveLazily = resolveLazily;
    }

    @Override
    protected FileUpload newFileUpload(FileItemFactory fileItemFactory) {
        return new ServletFileUpload(fileItemFactory);
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (!isUploadTempDirSpecified()) {
            getFileItemFactory().setRepository(WebUtils.getTempDir(servletContext));
        }
    }

    @Override
    public boolean isMultipart(HttpServletRequest request) {
        return ServletFileUpload.isMultipartContent(request);
    }

    @Override
    public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {
        Assert.notNull(request, "Request must not be null");
        if (this.resolveLazily) {
            return new DefaultMultipartHttpServletRequest(request) {
                @Override
                protected void initializeMultipart() {
                    MultipartParsingResult parsingResult = parseRequest(request);
                    setMultipartFiles(parsingResult.getMultipartFiles());
                    setMultipartParameters(parsingResult.getMultipartParameters());
                    setMultipartParameterContentTypes(parsingResult.getMultipartParameterContentTypes());
                }
            };
        } else {
            MultipartParsingResult parsingResult = parseRequest(request);
            return new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(), parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());
        }
    }

    protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
        String encoding = determineEncoding(request);
        FileUpload fileUpload = prepareFileUpload(encoding);
        try {
            List<FileItem> fileItems = ((ServletFileUpload) fileUpload).parseRequest(request);
            return parseFileItems(fileItems, encoding);
        } catch (FileUploadBase.SizeLimitExceededException ex) {
            throw new MaxUploadSizeExceededException(fileUpload.getSizeMax(), ex);
        } catch (FileUploadBase.FileSizeLimitExceededException ex) {
            throw new MaxUploadSizeExceededException(fileUpload.getFileSizeMax(), ex);
        } catch (FileUploadException ex) {
            throw new MultipartException("Failed to parse multipart servlet request", ex);
        }
    }

    protected String determineEncoding(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        if (encoding == null) {
            encoding = getDefaultEncoding();
        }
        return encoding;
    }

    @Override
    public void cleanupMultipart(MultipartHttpServletRequest request) {
        if (!(request instanceof AbstractMultipartHttpServletRequest) || ((AbstractMultipartHttpServletRequest) request).isResolved()) {
            try {
                cleanupFileItems(request.getMultiFileMap());
            } catch (Throwable ex) {
                logger.warn("Failed to perform multipart cleanup for servlet request", ex);
            }
        }
    }

}
