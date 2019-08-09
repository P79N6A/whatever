package org.springframework.http.converter.xml;

import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.Assert;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractJaxb2HttpMessageConverter<T> extends AbstractXmlHttpMessageConverter<T> {

    private final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>(64);

    protected final Marshaller createMarshaller(Class<?> clazz) {
        try {
            JAXBContext jaxbContext = getJaxbContext(clazz);
            Marshaller marshaller = jaxbContext.createMarshaller();
            customizeMarshaller(marshaller);
            return marshaller;
        } catch (JAXBException ex) {
            throw new HttpMessageConversionException("Could not create Marshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
        }
    }

    protected void customizeMarshaller(Marshaller marshaller) {
    }

    protected final Unmarshaller createUnmarshaller(Class<?> clazz) {
        try {
            JAXBContext jaxbContext = getJaxbContext(clazz);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            customizeUnmarshaller(unmarshaller);
            return unmarshaller;
        } catch (JAXBException ex) {
            throw new HttpMessageConversionException("Could not create Unmarshaller for class [" + clazz + "]: " + ex.getMessage(), ex);
        }
    }

    protected void customizeUnmarshaller(Unmarshaller unmarshaller) {
    }

    protected final JAXBContext getJaxbContext(Class<?> clazz) {
        Assert.notNull(clazz, "Class must not be null");
        JAXBContext jaxbContext = this.jaxbContexts.get(clazz);
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(clazz);
                this.jaxbContexts.putIfAbsent(clazz, jaxbContext);
            } catch (JAXBException ex) {
                throw new HttpMessageConversionException("Could not instantiate JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
            }
        }
        return jaxbContext;
    }

}
