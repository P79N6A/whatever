package org.springframework.web.servlet.view.xslt;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.SimpleTransformErrorListener;
import org.springframework.util.xml.TransformerUtils;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.util.WebUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

public class XsltView extends AbstractUrlBasedView {

    @Nullable
    private Class<? extends TransformerFactory> transformerFactoryClass;

    @Nullable
    private String sourceKey;

    @Nullable
    private URIResolver uriResolver;

    private ErrorListener errorListener = new SimpleTransformErrorListener(logger);

    private boolean indent = true;

    @Nullable
    private Properties outputProperties;

    private boolean cacheTemplates = true;

    @Nullable
    private TransformerFactory transformerFactory;

    @Nullable
    private Templates cachedTemplates;

    public void setTransformerFactoryClass(Class<? extends TransformerFactory> transformerFactoryClass) {
        this.transformerFactoryClass = transformerFactoryClass;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public void setUriResolver(URIResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

    public void setErrorListener(@Nullable ErrorListener errorListener) {
        this.errorListener = (errorListener != null ? errorListener : new SimpleTransformErrorListener(logger));
    }

    public void setIndent(boolean indent) {
        this.indent = indent;
    }

    public void setOutputProperties(Properties outputProperties) {
        this.outputProperties = outputProperties;
    }

    public void setCacheTemplates(boolean cacheTemplates) {
        this.cacheTemplates = cacheTemplates;
    }

    @Override
    protected void initApplicationContext() throws BeansException {
        this.transformerFactory = newTransformerFactory(this.transformerFactoryClass);
        this.transformerFactory.setErrorListener(this.errorListener);
        if (this.uriResolver != null) {
            this.transformerFactory.setURIResolver(this.uriResolver);
        }
        if (this.cacheTemplates) {
            this.cachedTemplates = loadTemplates();
        }
    }

    protected TransformerFactory newTransformerFactory(@Nullable Class<? extends TransformerFactory> transformerFactoryClass) {
        if (transformerFactoryClass != null) {
            try {
                return ReflectionUtils.accessibleConstructor(transformerFactoryClass).newInstance();
            } catch (Exception ex) {
                throw new TransformerFactoryConfigurationError(ex, "Could not instantiate TransformerFactory");
            }
        } else {
            return TransformerFactory.newInstance();
        }
    }

    protected final TransformerFactory getTransformerFactory() {
        Assert.state(this.transformerFactory != null, "No TransformerFactory available");
        return this.transformerFactory;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Templates templates = this.cachedTemplates;
        if (templates == null) {
            templates = loadTemplates();
        }
        Transformer transformer = createTransformer(templates);
        configureTransformer(model, response, transformer);
        configureResponse(model, response, transformer);
        Source source = null;
        try {
            source = locateSource(model);
            if (source == null) {
                throw new IllegalArgumentException("Unable to locate Source object in model: " + model);
            }
            transformer.transform(source, createResult(response));
        } finally {
            closeSourceIfNecessary(source);
        }
    }

    protected Result createResult(HttpServletResponse response) throws Exception {
        return new StreamResult(response.getOutputStream());
    }

    @Nullable
    protected Source locateSource(Map<String, Object> model) throws Exception {
        if (this.sourceKey != null) {
            return convertSource(model.get(this.sourceKey));
        }
        Object source = CollectionUtils.findValueOfType(model.values(), getSourceTypes());
        return (source != null ? convertSource(source) : null);
    }

    protected Class<?>[] getSourceTypes() {
        return new Class<?>[]{Source.class, Document.class, Node.class, Reader.class, InputStream.class, Resource.class};
    }

    protected Source convertSource(Object source) throws Exception {
        if (source instanceof Source) {
            return (Source) source;
        } else if (source instanceof Document) {
            return new DOMSource(((Document) source).getDocumentElement());
        } else if (source instanceof Node) {
            return new DOMSource((Node) source);
        } else if (source instanceof Reader) {
            return new StreamSource((Reader) source);
        } else if (source instanceof InputStream) {
            return new StreamSource((InputStream) source);
        } else if (source instanceof Resource) {
            Resource resource = (Resource) source;
            return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
        } else {
            throw new IllegalArgumentException("Value '" + source + "' cannot be converted to XSLT Source");
        }
    }

    protected void configureTransformer(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
        copyModelParameters(model, transformer);
        copyOutputProperties(transformer);
        configureIndentation(transformer);
    }

    protected final void configureIndentation(Transformer transformer) {
        if (this.indent) {
            TransformerUtils.enableIndenting(transformer);
        } else {
            TransformerUtils.disableIndenting(transformer);
        }
    }

    protected final void copyOutputProperties(Transformer transformer) {
        if (this.outputProperties != null) {
            Enumeration<?> en = this.outputProperties.propertyNames();
            while (en.hasMoreElements()) {
                String name = (String) en.nextElement();
                transformer.setOutputProperty(name, this.outputProperties.getProperty(name));
            }
        }
    }

    protected final void copyModelParameters(Map<String, Object> model, Transformer transformer) {
        model.forEach(transformer::setParameter);
    }

    protected void configureResponse(Map<String, Object> model, HttpServletResponse response, Transformer transformer) {
        String contentType = getContentType();
        String mediaType = transformer.getOutputProperty(OutputKeys.MEDIA_TYPE);
        String encoding = transformer.getOutputProperty(OutputKeys.ENCODING);
        if (StringUtils.hasText(mediaType)) {
            contentType = mediaType;
        }
        if (StringUtils.hasText(encoding)) {
            // Only apply encoding if content type is specified but does not contain charset clause already.
            if (contentType != null && !contentType.toLowerCase().contains(WebUtils.CONTENT_TYPE_CHARSET_PREFIX)) {
                contentType = contentType + WebUtils.CONTENT_TYPE_CHARSET_PREFIX + encoding;
            }
        }
        response.setContentType(contentType);
    }

    private Templates loadTemplates() throws ApplicationContextException {
        Source stylesheetSource = getStylesheetSource();
        try {
            Templates templates = getTransformerFactory().newTemplates(stylesheetSource);
            return templates;
        } catch (TransformerConfigurationException ex) {
            throw new ApplicationContextException("Can't load stylesheet from '" + getUrl() + "'", ex);
        } finally {
            closeSourceIfNecessary(stylesheetSource);
        }
    }

    protected Transformer createTransformer(Templates templates) throws TransformerConfigurationException {
        Transformer transformer = templates.newTransformer();
        if (this.uriResolver != null) {
            transformer.setURIResolver(this.uriResolver);
        }
        return transformer;
    }

    protected Source getStylesheetSource() {
        String url = getUrl();
        Assert.state(url != null, "'url' not set");
        if (logger.isDebugEnabled()) {
            logger.debug("Applying stylesheet [" + url + "]");
        }
        try {
            Resource resource = obtainApplicationContext().getResource(url);
            return new StreamSource(resource.getInputStream(), resource.getURI().toASCIIString());
        } catch (IOException ex) {
            throw new ApplicationContextException("Can't load XSLT stylesheet from '" + url + "'", ex);
        }
    }

    private void closeSourceIfNecessary(@Nullable Source source) {
        if (source instanceof StreamSource) {
            StreamSource streamSource = (StreamSource) source;
            if (streamSource.getReader() != null) {
                try {
                    streamSource.getReader().close();
                } catch (IOException ex) {
                    // ignore
                }
            }
            if (streamSource.getInputStream() != null) {
                try {
                    streamSource.getInputStream().close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

}
