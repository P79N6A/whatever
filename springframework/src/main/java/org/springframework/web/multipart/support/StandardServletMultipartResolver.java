package org.springframework.web.multipart.support;

import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public class StandardServletMultipartResolver implements MultipartResolver {

    private boolean resolveLazily = false;

    public void setResolveLazily(boolean resolveLazily) {
        this.resolveLazily = resolveLazily;
    }

    @Override
    public boolean isMultipart(HttpServletRequest request) {
        return StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/");
    }

    @Override
    public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
        return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
    }

    @Override
    public void cleanupMultipart(MultipartHttpServletRequest request) {
        if (!(request instanceof AbstractMultipartHttpServletRequest) || ((AbstractMultipartHttpServletRequest) request).isResolved()) {
            // To be on the safe side: explicitly delete the parts,
            // but only actual file parts (for Resin compatibility)
            try {
                for (Part part : request.getParts()) {
                    if (request.getFile(part.getName()) != null) {
                        part.delete();
                    }
                }
            } catch (Throwable ex) {
                LogFactory.getLog(getClass()).warn("Failed to perform cleanup of multipart items", ex);
            }
        }
    }

}
