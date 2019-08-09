package org.springframework.util.xml;

import org.apache.commons.logging.Log;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

public class SimpleTransformErrorListener implements ErrorListener {

    private final Log logger;

    public SimpleTransformErrorListener(Log logger) {
        this.logger = logger;
    }

    @Override
    public void warning(TransformerException ex) throws TransformerException {
        logger.warn("XSLT transformation warning", ex);
    }

    @Override
    public void error(TransformerException ex) throws TransformerException {
        logger.error("XSLT transformation error", ex);
    }

    @Override
    public void fatalError(TransformerException ex) throws TransformerException {
        throw ex;
    }

}
