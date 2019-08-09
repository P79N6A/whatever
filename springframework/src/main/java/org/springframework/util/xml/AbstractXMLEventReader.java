package org.springframework.util.xml;

import org.springframework.util.ClassUtils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import java.util.NoSuchElementException;

abstract class AbstractXMLEventReader implements XMLEventReader {

    private boolean closed;

    @Override
    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException ex) {
            throw new NoSuchElementException(ex.getMessage());
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported on " + ClassUtils.getShortName(getClass()));
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new IllegalArgumentException("Property not supported: [" + name + "]");
    }

    @Override
    public void close() {
        this.closed = true;
    }

    protected void checkIfClosed() throws XMLStreamException {
        if (this.closed) {
            throw new XMLStreamException("XMLEventReader has been closed");
        }
    }

}
