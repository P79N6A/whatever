package org.springframework.util.xml;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Map;

class StaxStreamHandler extends AbstractStaxHandler {

    private final XMLStreamWriter streamWriter;

    public StaxStreamHandler(XMLStreamWriter streamWriter) {
        this.streamWriter = streamWriter;
    }

    @Override
    protected void startDocumentInternal() throws XMLStreamException {
        this.streamWriter.writeStartDocument();
    }

    @Override
    protected void endDocumentInternal() throws XMLStreamException {
        this.streamWriter.writeEndDocument();
    }

    @Override
    protected void startElementInternal(QName name, Attributes attributes, Map<String, String> namespaceMapping) throws XMLStreamException {
        this.streamWriter.writeStartElement(name.getPrefix(), name.getLocalPart(), name.getNamespaceURI());
        for (Map.Entry<String, String> entry : namespaceMapping.entrySet()) {
            String prefix = entry.getKey();
            String namespaceUri = entry.getValue();
            this.streamWriter.writeNamespace(prefix, namespaceUri);
            if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
                this.streamWriter.setDefaultNamespace(namespaceUri);
            } else {
                this.streamWriter.setPrefix(prefix, namespaceUri);
            }
        }
        for (int i = 0; i < attributes.getLength(); i++) {
            QName attrName = toQName(attributes.getURI(i), attributes.getQName(i));
            if (!isNamespaceDeclaration(attrName)) {
                this.streamWriter.writeAttribute(attrName.getPrefix(), attrName.getNamespaceURI(), attrName.getLocalPart(), attributes.getValue(i));
            }
        }
    }

    @Override
    protected void endElementInternal(QName name, Map<String, String> namespaceMapping) throws XMLStreamException {
        this.streamWriter.writeEndElement();
    }

    @Override
    protected void charactersInternal(String data) throws XMLStreamException {
        this.streamWriter.writeCharacters(data);
    }

    @Override
    protected void cDataInternal(String data) throws XMLStreamException {
        this.streamWriter.writeCData(data);
    }

    @Override
    protected void ignorableWhitespaceInternal(String data) throws XMLStreamException {
        this.streamWriter.writeCharacters(data);
    }

    @Override
    protected void processingInstructionInternal(String target, String data) throws XMLStreamException {
        this.streamWriter.writeProcessingInstruction(target, data);
    }

    @Override
    protected void dtdInternal(String dtd) throws XMLStreamException {
        this.streamWriter.writeDTD(dtd);
    }

    @Override
    protected void commentInternal(String comment) throws XMLStreamException {
        this.streamWriter.writeComment(comment);
    }
    // Ignored

    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startEntity(String name) throws SAXException {
    }

    @Override
    public void endEntity(String name) throws SAXException {
    }

    @Override
    protected void skippedEntityInternal(String name) throws XMLStreamException {
    }

}
