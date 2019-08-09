package org.springframework.beans.factory.xml;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.AbstractBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Constants;
import org.springframework.core.NamedThreadLocal;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.xml.SimpleSaxErrorHandler;
import org.springframework.util.xml.XmlValidationModeDetector;
import org.w3c.dom.Document;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class XmlBeanDefinitionReader extends AbstractBeanDefinitionReader {

    public static final int VALIDATION_NONE = XmlValidationModeDetector.VALIDATION_NONE;

    public static final int VALIDATION_AUTO = XmlValidationModeDetector.VALIDATION_AUTO;

    public static final int VALIDATION_DTD = XmlValidationModeDetector.VALIDATION_DTD;

    public static final int VALIDATION_XSD = XmlValidationModeDetector.VALIDATION_XSD;

    private static final Constants constants = new Constants(XmlBeanDefinitionReader.class);

    private int validationMode = VALIDATION_AUTO;

    private boolean namespaceAware = false;

    private Class<? extends BeanDefinitionDocumentReader> documentReaderClass = DefaultBeanDefinitionDocumentReader.class;

    private ProblemReporter problemReporter = new FailFastProblemReporter();

    private ReaderEventListener eventListener = new EmptyReaderEventListener();

    private SourceExtractor sourceExtractor = new NullSourceExtractor();

    @Nullable
    private NamespaceHandlerResolver namespaceHandlerResolver;

    private DocumentLoader documentLoader = new DefaultDocumentLoader();

    @Nullable
    private EntityResolver entityResolver;

    private ErrorHandler errorHandler = new SimpleSaxErrorHandler(logger);

    private final XmlValidationModeDetector validationModeDetector = new XmlValidationModeDetector();

    private final ThreadLocal<Set<EncodedResource>> resourcesCurrentlyBeingLoaded = new NamedThreadLocal<>("XML bean definition resources currently being loaded");

    public XmlBeanDefinitionReader(BeanDefinitionRegistry registry) {
        super(registry);
    }

    public void setValidating(boolean validating) {
        this.validationMode = (validating ? VALIDATION_AUTO : VALIDATION_NONE);
        this.namespaceAware = !validating;
    }

    public void setValidationModeName(String validationModeName) {
        setValidationMode(constants.asNumber(validationModeName).intValue());
    }

    public void setValidationMode(int validationMode) {
        this.validationMode = validationMode;
    }

    public int getValidationMode() {
        return this.validationMode;
    }

    public void setNamespaceAware(boolean namespaceAware) {
        this.namespaceAware = namespaceAware;
    }

    public boolean isNamespaceAware() {
        return this.namespaceAware;
    }

    public void setProblemReporter(@Nullable ProblemReporter problemReporter) {
        this.problemReporter = (problemReporter != null ? problemReporter : new FailFastProblemReporter());
    }

    public void setEventListener(@Nullable ReaderEventListener eventListener) {
        this.eventListener = (eventListener != null ? eventListener : new EmptyReaderEventListener());
    }

    public void setSourceExtractor(@Nullable SourceExtractor sourceExtractor) {
        this.sourceExtractor = (sourceExtractor != null ? sourceExtractor : new NullSourceExtractor());
    }

    public void setNamespaceHandlerResolver(@Nullable NamespaceHandlerResolver namespaceHandlerResolver) {
        this.namespaceHandlerResolver = namespaceHandlerResolver;
    }

    public void setDocumentLoader(@Nullable DocumentLoader documentLoader) {
        this.documentLoader = (documentLoader != null ? documentLoader : new DefaultDocumentLoader());
    }

    public void setEntityResolver(@Nullable EntityResolver entityResolver) {
        this.entityResolver = entityResolver;
    }

    protected EntityResolver getEntityResolver() {
        if (this.entityResolver == null) {
            // Determine default EntityResolver to use.
            ResourceLoader resourceLoader = getResourceLoader();
            if (resourceLoader != null) {
                this.entityResolver = new ResourceEntityResolver(resourceLoader);
            } else {
                this.entityResolver = new DelegatingEntityResolver(getBeanClassLoader());
            }
        }
        return this.entityResolver;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setDocumentReaderClass(Class<? extends BeanDefinitionDocumentReader> documentReaderClass) {
        this.documentReaderClass = documentReaderClass;
    }

    @Override
    public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(new EncodedResource(resource));
    }

    public int loadBeanDefinitions(EncodedResource encodedResource) throws BeanDefinitionStoreException {
        Assert.notNull(encodedResource, "EncodedResource must not be null");
        if (logger.isTraceEnabled()) {
            logger.trace("Loading XML bean definitions from " + encodedResource);
        }
        // 用ThreadLocal标记
        Set<EncodedResource> currentResources = this.resourcesCurrentlyBeingLoaded.get();
        if (currentResources == null) {
            currentResources = new HashSet<>(4);
            this.resourcesCurrentlyBeingLoaded.set(currentResources);
        }
        // 已经在处理，抛循环导入异常
        if (!currentResources.add(encodedResource)) {
            throw new BeanDefinitionStoreException("Detected cyclic loading of " + encodedResource + " - check your import definitions!");
        }
        try {
            InputStream inputStream = encodedResource.getResource().getInputStream();
            try {
                InputSource inputSource = new InputSource(inputStream);
                if (encodedResource.getEncoding() != null) {
                    inputSource.setEncoding(encodedResource.getEncoding());
                }
                // 核心
                return doLoadBeanDefinitions(inputSource, encodedResource.getResource());
            } finally {
                inputStream.close();
            }
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("IOException parsing XML document from " + encodedResource.getResource(), ex);
        } finally {
            // 加载完移除
            currentResources.remove(encodedResource);
            if (currentResources.isEmpty()) {
                // 清空ThreadLocal
                this.resourcesCurrentlyBeingLoaded.remove();
            }
        }
    }

    public int loadBeanDefinitions(InputSource inputSource) throws BeanDefinitionStoreException {
        return loadBeanDefinitions(inputSource, "resource loaded through SAX InputSource");
    }

    public int loadBeanDefinitions(InputSource inputSource, @Nullable String resourceDescription) throws BeanDefinitionStoreException {
        return doLoadBeanDefinitions(inputSource, new DescriptiveResource(resourceDescription));
    }

    protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource) throws BeanDefinitionStoreException {
        try {
            // Xml转Document
            Document doc = doLoadDocument(inputSource, resource);
            // 注册BeanDefinition
            int count = registerBeanDefinitions(doc, resource);
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded " + count + " bean definitions from " + resource);
            }
            // 返回注册数量
            return count;
        } catch (BeanDefinitionStoreException ex) {
            throw ex;
        } catch (SAXParseException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(), "Line " + ex.getLineNumber() + " in XML document from " + resource + " is invalid", ex);
        } catch (SAXException ex) {
            throw new XmlBeanDefinitionStoreException(resource.getDescription(), "XML document from " + resource + " is invalid", ex);
        } catch (ParserConfigurationException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(), "Parser configuration exception parsing XML from " + resource, ex);
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(), "IOException parsing XML document from " + resource, ex);
        } catch (Throwable ex) {
            throw new BeanDefinitionStoreException(resource.getDescription(), "Unexpected exception parsing XML document from " + resource, ex);
        }
    }

    protected Document doLoadDocument(InputSource inputSource, Resource resource) throws Exception {
        return this.documentLoader.loadDocument(inputSource, getEntityResolver(), this.errorHandler, getValidationModeForResource(resource), isNamespaceAware());
    }

    protected int getValidationModeForResource(Resource resource) {
        int validationModeToUse = getValidationMode();
        if (validationModeToUse != VALIDATION_AUTO) {
            return validationModeToUse;
        }
        int detectedMode = detectValidationMode(resource);
        if (detectedMode != VALIDATION_AUTO) {
            return detectedMode;
        }
        // Hmm, we didn't get a clear indication... Let's assume XSD,
        // since apparently no DTD declaration has been found up until
        // detection stopped (before finding the document's root tag).
        return VALIDATION_XSD;
    }

    protected int detectValidationMode(Resource resource) {
        if (resource.isOpen()) {
            throw new BeanDefinitionStoreException("Passed-in Resource [" + resource + "] contains an open stream: " + "cannot determine validation mode automatically. Either pass in a Resource " + "that is able to create fresh streams, or explicitly specify the validationMode " + "on your XmlBeanDefinitionReader instance.");
        }
        InputStream inputStream;
        try {
            inputStream = resource.getInputStream();
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("Unable to determine validation mode for [" + resource + "]: cannot open InputStream. " + "Did you attempt to load directly from a SAX InputSource without specifying the " + "validationMode on your XmlBeanDefinitionReader instance?", ex);
        }
        try {
            return this.validationModeDetector.detectValidationMode(inputStream);
        } catch (IOException ex) {
            throw new BeanDefinitionStoreException("Unable to determine validation mode for [" + resource + "]: an error occurred whilst reading from the InputStream.", ex);
        }
    }

    /**
     * XmlBeanDefinitionReader：将Xml解析成为Document
     * BeanDefinitionDocumentReader：解析Document为BeanDefinition并注册到BeanFactory
     */
    public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStoreException {
        BeanDefinitionDocumentReader documentReader = createBeanDefinitionDocumentReader();
        // 注册前的数量
        int countBefore = getRegistry().getBeanDefinitionCount();
        // 注册
        documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
        // 返回从当前配置文件加载了多少Bean
        return getRegistry().getBeanDefinitionCount() - countBefore;
    }

    protected BeanDefinitionDocumentReader createBeanDefinitionDocumentReader() {
        return BeanUtils.instantiateClass(this.documentReaderClass);
    }

    public XmlReaderContext createReaderContext(Resource resource) {
        return new XmlReaderContext(resource, this.problemReporter, this.eventListener, this.sourceExtractor, this, getNamespaceHandlerResolver());
    }

    public NamespaceHandlerResolver getNamespaceHandlerResolver() {
        if (this.namespaceHandlerResolver == null) {
            this.namespaceHandlerResolver = createDefaultNamespaceHandlerResolver();
        }
        return this.namespaceHandlerResolver;
    }

    protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
        ClassLoader cl = (getResourceLoader() != null ? getResourceLoader().getClassLoader() : getBeanClassLoader());
        return new DefaultNamespaceHandlerResolver(cl);
    }

}
