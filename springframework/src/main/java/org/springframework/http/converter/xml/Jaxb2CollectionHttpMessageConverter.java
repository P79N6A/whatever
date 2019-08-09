package org.springframework.http.converter.xml;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.xml.StaxUtils;

import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@SuppressWarnings("rawtypes")
public class Jaxb2CollectionHttpMessageConverter<T extends Collection> extends AbstractJaxb2HttpMessageConverter<T> implements GenericHttpMessageConverter<T> {

    private final XMLInputFactory inputFactory = createXmlInputFactory();

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canRead(Type type, @Nullable Class<?> contextClass, @Nullable MediaType mediaType) {
        if (!(type instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        if (!(parameterizedType.getRawType() instanceof Class)) {
            return false;
        }
        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
        if (!(Collection.class.isAssignableFrom(rawType))) {
            return false;
        }
        if (parameterizedType.getActualTypeArguments().length != 1) {
            return false;
        }
        Type typeArgument = parameterizedType.getActualTypeArguments()[0];
        if (!(typeArgument instanceof Class)) {
            return false;
        }
        Class<?> typeArgumentClass = (Class<?>) typeArgument;
        return (typeArgumentClass.isAnnotationPresent(XmlRootElement.class) || typeArgumentClass.isAnnotationPresent(XmlType.class)) && canRead(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(@Nullable Type type, @Nullable Class<?> clazz, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // should not be called, since we override canRead/Write
        throw new UnsupportedOperationException();
    }

    @Override
    protected T readFromSource(Class<? extends T> clazz, HttpHeaders headers, Source source) throws Exception {
        // should not be called, since we return false for canRead(Class)
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T read(Type type, @Nullable Class<?> contextClass, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        T result = createCollection((Class<?>) parameterizedType.getRawType());
        Class<?> elementClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        try {
            Unmarshaller unmarshaller = createUnmarshaller(elementClass);
            XMLStreamReader streamReader = this.inputFactory.createXMLStreamReader(inputMessage.getBody());
            int event = moveToFirstChildOfRootElement(streamReader);
            while (event != XMLStreamReader.END_DOCUMENT) {
                if (elementClass.isAnnotationPresent(XmlRootElement.class)) {
                    result.add(unmarshaller.unmarshal(streamReader));
                } else if (elementClass.isAnnotationPresent(XmlType.class)) {
                    result.add(unmarshaller.unmarshal(streamReader, elementClass).getValue());
                } else {
                    // should not happen, since we check in canRead(Type)
                    throw new HttpMessageNotReadableException("Cannot unmarshal to [" + elementClass + "]", inputMessage);
                }
                event = moveToNextElement(streamReader);
            }
            return result;
        } catch (XMLStreamException ex) {
            throw new HttpMessageNotReadableException("Failed to read XML stream: " + ex.getMessage(), ex, inputMessage);
        } catch (UnmarshalException ex) {
            throw new HttpMessageNotReadableException("Could not unmarshal to [" + elementClass + "]: " + ex.getMessage(), ex, inputMessage);
        } catch (JAXBException ex) {
            throw new HttpMessageConversionException("Invalid JAXB setup: " + ex.getMessage(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    protected T createCollection(Class<?> collectionClass) {
        if (!collectionClass.isInterface()) {
            try {
                return (T) ReflectionUtils.accessibleConstructor(collectionClass).newInstance();
            } catch (Throwable ex) {
                throw new IllegalArgumentException("Could not instantiate collection class: " + collectionClass.getName(), ex);
            }
        } else if (List.class == collectionClass) {
            return (T) new ArrayList();
        } else if (SortedSet.class == collectionClass) {
            return (T) new TreeSet();
        } else {
            return (T) new LinkedHashSet();
        }
    }

    private int moveToFirstChildOfRootElement(XMLStreamReader streamReader) throws XMLStreamException {
        // root
        int event = streamReader.next();
        while (event != XMLStreamReader.START_ELEMENT) {
            event = streamReader.next();
        }
        // first child
        event = streamReader.next();
        while ((event != XMLStreamReader.START_ELEMENT) && (event != XMLStreamReader.END_DOCUMENT)) {
            event = streamReader.next();
        }
        return event;
    }

    private int moveToNextElement(XMLStreamReader streamReader) throws XMLStreamException {
        int event = streamReader.getEventType();
        while (event != XMLStreamReader.START_ELEMENT && event != XMLStreamReader.END_DOCUMENT) {
            event = streamReader.next();
        }
        return event;
    }

    @Override
    public void write(T t, @Nullable Type type, @Nullable MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeToResult(T t, HttpHeaders headers, Result result) throws Exception {
        throw new UnsupportedOperationException();
    }

    protected XMLInputFactory createXmlInputFactory() {
        return StaxUtils.createDefensiveInputFactory();
    }

}
