package org.springframework.util.xml;

import org.springframework.lang.Nullable;
import org.xml.sax.*;
import org.xml.sax.ext.LexicalHandler;

abstract class AbstractXMLReader implements XMLReader {

    @Nullable
    private DTDHandler dtdHandler;

    @Nullable
    private ContentHandler contentHandler;

    @Nullable
    private EntityResolver entityResolver;

    @Nullable
    private ErrorHandler errorHandler;

    @Nullable
    private LexicalHandler lexicalHandler;

    @Override
    public void setContentHandler(@Nullable ContentHandler contentHandler) {
        this.contentHandler = contentHandler;
    }

    @Override
    @Nullable
    public ContentHandler getContentHandler() {
        return this.contentHandler;
    }

    @Override
    public void setDTDHandler(@Nullable DTDHandler dtdHandler) {
        this.dtdHandler = dtdHandler;
    }

    @Override
    @Nullable
    public DTDHandler getDTDHandler() {
        return this.dtdHandler;
    }

    @Override
    public void setEntityResolver(@Nullable EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    @Override
    @Nullable
    public EntityResolver getEntityResolver() {
        return this.entityResolver;
    }

    @Override
    public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    @Nullable
    public ErrorHandler getErrorHandler() {
        return this.errorHandler;
    }

    @Nullable
    protected LexicalHandler getLexicalHandler() {
        return this.lexicalHandler;
    }

    @Override
    public boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.startsWith("http://xml.org/sax/features/")) {
            return false;
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }

    @Override
    public void setFeature(String name, boolean value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.startsWith("http://xml.org/sax/features/")) {
            if (value) {
                throw new SAXNotSupportedException(name);
            }
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }

    @Override
    @Nullable
    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            return this.lexicalHandler;
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }

    @Override
    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
        if ("http://xml.org/sax/properties/lexical-handler".equals(name)) {
            this.lexicalHandler = (LexicalHandler) value;
        } else {
            throw new SAXNotRecognizedException(name);
        }
    }

}
