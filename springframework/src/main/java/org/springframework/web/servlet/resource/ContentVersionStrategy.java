package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.util.DigestUtils;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;

public class ContentVersionStrategy extends AbstractVersionStrategy {

    public ContentVersionStrategy() {
        super(new FileNameVersionPathStrategy());
    }

    @Override
    public String getResourceVersion(Resource resource) {
        try {
            byte[] content = FileCopyUtils.copyToByteArray(resource.getInputStream());
            return DigestUtils.md5DigestAsHex(content);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to calculate hash for " + resource, ex);
        }
    }

}
