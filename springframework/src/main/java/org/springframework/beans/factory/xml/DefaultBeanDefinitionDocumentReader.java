package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.Set;

public class DefaultBeanDefinitionDocumentReader implements BeanDefinitionDocumentReader {

    public static final String BEAN_ELEMENT = BeanDefinitionParserDelegate.BEAN_ELEMENT;

    public static final String NESTED_BEANS_ELEMENT = "beans";

    public static final String ALIAS_ELEMENT = "alias";

    public static final String NAME_ATTRIBUTE = "name";

    public static final String ALIAS_ATTRIBUTE = "alias";

    public static final String IMPORT_ELEMENT = "import";

    public static final String RESOURCE_ATTRIBUTE = "resource";

    public static final String PROFILE_ATTRIBUTE = "profile";

    protected final Log logger = LogFactory.getLog(getClass());

    @Nullable
    private XmlReaderContext readerContext;

    @Nullable
    private BeanDefinitionParserDelegate delegate;

    @Override
    public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext) {
        this.readerContext = readerContext;
        // 从根节点开始解析
        doRegisterBeanDefinitions(doc.getDocumentElement());
    }

    protected final XmlReaderContext getReaderContext() {
        Assert.state(this.readerContext != null, "No XmlReaderContext available");
        return this.readerContext;
    }

    @Nullable
    protected Object extractSource(Element ele) {
        return getReaderContext().extractSource(ele);
    }

    @SuppressWarnings("deprecation")  // for Environment.acceptsProfiles(String...)
    protected void doRegisterBeanDefinitions(Element root) {
        // Any nested <beans> elements will cause recursion in this method. In
        // order to propagate and preserve <beans> default-* attributes correctly,
        // keep track of the current (parent) delegate, which may be null. Create
        // the new (child) delegate with a reference to the parent for fallback purposes,
        // then ultimately reset this.delegate back to its original (parent) reference.
        // this behavior emulates a stack of delegates without actually necessitating one.
        // 负责解析Bean定义，定义parent处理递归，<beans/>内部可以有<beans/>，root不一定是xml根节点，也可以是嵌套的<beans/>节点
        BeanDefinitionParserDelegate parent = this.delegate;
        this.delegate = createDelegate(getReaderContext(), root, parent);
        if (this.delegate.isDefaultNamespace(root)) {
            // 根节点<beans ... profile="dev"/>的profile
            String profileSpec = root.getAttribute(PROFILE_ATTRIBUTE);
            if (StringUtils.hasText(profileSpec)) {
                String[] specifiedProfiles = StringUtils.tokenizeToStringArray(profileSpec, BeanDefinitionParserDelegate.MULTI_VALUE_ATTRIBUTE_DELIMITERS);
                // We cannot use Profiles.of(...) since profile expressions are not supported
                // in XML config. See SPR-12458 for details.
                // 如果当前环境配置的profile不包含此profile，直接return，不解析此<beans/>
                if (!getReaderContext().getEnvironment().acceptsProfiles(specifiedProfiles)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Skipped XML bean definition file due to specified profiles [" + profileSpec + "] not matching: " + getReaderContext().getResource());
                    }
                    return;
                }
            }
        }
        // 预处理
        preProcessXml(root);
        // 解析
        parseBeanDefinitions(root, this.delegate);
        // 后处理
        postProcessXml(root);
        this.delegate = parent;
    }

    protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root, @Nullable BeanDefinitionParserDelegate parentDelegate) {
        BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    /**
     * 如果是默认标签，在DefaultBeanDefinitionDocumentReader中进行解析
     * 否则需要BeanDefinitionParserDelegate根据命名空间找到对应的NamespaceHandler
     */
    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate) {
        if (delegate.isDefaultNamespace(root)) {
            NodeList nl = root.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (node instanceof Element) {
                    Element ele = (Element) node;
                    /*
                     * 解析DefaultNamespace的元素：<import/>、<alias/>、<bean/>、<beans/>
                     *
                     * <beans xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     *        xmlns="http://www.springframework.org/schema/beans"
                     *        xsi:schemaLocation="http://www.springframework.org/schema/beans
                     *              http://www.springframework.org/schema/beans/spring-beans.xsd"
                     *        default-autowire="byName">
                     */
                    if (delegate.isDefaultNamespace(ele)) {
                        parseDefaultElement(ele, delegate);
                    }
                    /*
                     * 解析其他Namespace的元素：<mvc/>、<task/>、<context/>、<aop/>等
                     * Xml头要引入相应的namespace和.xsd文件的路径
                     * 同时提供相应NamespaceHandler，如MvcNamespaceHandler、TaskNamespaceHandler、ContextNamespaceHandler、AopNamespaceHandler等
                     */
                    else {
                        delegate.parseCustomElement(ele);
                    }
                }
            }
        } else {
            delegate.parseCustomElement(root);
        }
    }

    private void parseDefaultElement(Element ele, BeanDefinitionParserDelegate delegate) {
        // 处理<import/>
        if (delegate.nodeNameEquals(ele, IMPORT_ELEMENT)) {
            importBeanDefinitionResource(ele);
        }
        // 处理<alias/>
        else if (delegate.nodeNameEquals(ele, ALIAS_ELEMENT)) {
            processAliasRegistration(ele);
        }
        // 处理<bean/>
        else if (delegate.nodeNameEquals(ele, BEAN_ELEMENT)) {
            processBeanDefinition(ele, delegate);
        }
        // 嵌套的<beans/>
        else if (delegate.nodeNameEquals(ele, NESTED_BEANS_ELEMENT)) {
            // 递归
            doRegisterBeanDefinitions(ele);
        }
    }

    protected void importBeanDefinitionResource(Element ele) {
        String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
        if (!StringUtils.hasText(location)) {
            getReaderContext().error("Resource location must not be empty", ele);
            return;
        }
        // Resolve system properties: e.g. "${user.dir}"
        location = getReaderContext().getEnvironment().resolveRequiredPlaceholders(location);
        Set<Resource> actualResources = new LinkedHashSet<>(4);
        // Discover whether the location is an absolute or relative URI
        boolean absoluteLocation = false;
        try {
            absoluteLocation = ResourcePatternUtils.isUrl(location) || ResourceUtils.toURI(location).isAbsolute();
        } catch (URISyntaxException ex) {
            // cannot convert to an URI, considering the location relative
            // unless it is the well-known Spring prefix "classpath*:"
        }
        // Absolute or relative?
        if (absoluteLocation) {
            try {
                int importCount = getReaderContext().getReader().loadBeanDefinitions(location, actualResources);
                if (logger.isTraceEnabled()) {
                    logger.trace("Imported " + importCount + " bean definitions from URL location [" + location + "]");
                }
            } catch (BeanDefinitionStoreException ex) {
                getReaderContext().error("Failed to import bean definitions from URL location [" + location + "]", ele, ex);
            }
        } else {
            // No URL -> considering resource location as relative to the current file.
            try {
                int importCount;
                Resource relativeResource = getReaderContext().getResource().createRelative(location);
                if (relativeResource.exists()) {
                    importCount = getReaderContext().getReader().loadBeanDefinitions(relativeResource);
                    actualResources.add(relativeResource);
                } else {
                    String baseLocation = getReaderContext().getResource().getURL().toString();
                    importCount = getReaderContext().getReader().loadBeanDefinitions(StringUtils.applyRelativePath(baseLocation, location), actualResources);
                }
                if (logger.isTraceEnabled()) {
                    logger.trace("Imported " + importCount + " bean definitions from relative location [" + location + "]");
                }
            } catch (IOException ex) {
                getReaderContext().error("Failed to resolve current resource location", ele, ex);
            } catch (BeanDefinitionStoreException ex) {
                getReaderContext().error("Failed to import bean definitions from relative location [" + location + "]", ele, ex);
            }
        }
        Resource[] actResArray = actualResources.toArray(new Resource[0]);
        getReaderContext().fireImportProcessed(location, actResArray, extractSource(ele));
    }

    protected void processAliasRegistration(Element ele) {
        String name = ele.getAttribute(NAME_ATTRIBUTE);
        String alias = ele.getAttribute(ALIAS_ATTRIBUTE);
        boolean valid = true;
        if (!StringUtils.hasText(name)) {
            getReaderContext().error("Name must not be empty", ele);
            valid = false;
        }
        if (!StringUtils.hasText(alias)) {
            getReaderContext().error("Alias must not be empty", ele);
            valid = false;
        }
        if (valid) {
            try {
                getReaderContext().getRegistry().registerAlias(name, alias);
            } catch (Exception ex) {
                getReaderContext().error("Failed to register alias '" + alias + "' for bean with name '" + name + "'", ele, ex);
            }
            getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
        }
    }

    /**
     * 将BeanDefinition注册到BeanFactory
     */
    protected void processBeanDefinition(Element ele, BeanDefinitionParserDelegate delegate) {
        // 将<bean/>节点的信息提取到BeanDefinitionHolder
        // BeanDefinitionHolder是对BeanDefinition的封装，包括BeanDefinition，beanName，aliases
        BeanDefinitionHolder bdHolder = delegate.parseBeanDefinitionElement(ele);
        if (bdHolder != null) {
            // 如果有自定义属性的话，解析
            bdHolder = delegate.decorateBeanDefinitionIfRequired(ele, bdHolder);
            try {
                // 注册Bean，将BeanDefinition注册到BeanFactory
                BeanDefinitionReaderUtils.registerBeanDefinition(bdHolder, getReaderContext().getRegistry());
            } catch (BeanDefinitionStoreException ex) {
                getReaderContext().error("Failed to register bean definition with name '" + bdHolder.getBeanName() + "'", ele, ex);
            }
            // 注册完成后，发送事件
            getReaderContext().fireComponentRegistered(new BeanComponentDefinition(bdHolder));
        }
    }

    protected void preProcessXml(Element root) {
    }

    protected void postProcessXml(Element root) {
    }

}
