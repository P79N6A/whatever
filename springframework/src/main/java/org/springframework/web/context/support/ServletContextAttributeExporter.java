package org.springframework.web.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.lang.Nullable;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.Map;

public class ServletContextAttributeExporter implements ServletContextAware {

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private Map<String, Object> attributes;

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        if (this.attributes != null) {
            for (Map.Entry<String, Object> entry : this.attributes.entrySet()) {
                String attributeName = entry.getKey();
                if (logger.isDebugEnabled()) {
                    if (servletContext.getAttribute(attributeName) != null) {
                        logger.debug("Replacing existing ServletContext attribute with name '" + attributeName + "'");
                    }
                }
                servletContext.setAttribute(attributeName, entry.getValue());
                if (logger.isTraceEnabled()) {
                    logger.trace("Exported ServletContext attribute with name '" + attributeName + "'");
                }
            }
        }
    }

}
