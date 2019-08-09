package org.springframework.beans.factory.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanMetadataAttribute;
import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.parsing.*;
import org.springframework.beans.factory.support.*;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * 将Bean的各种标签解析成BeanDefinition对象，并组装成BeanDefinitionHolder返回
 */
public class BeanDefinitionParserDelegate {

    /**
     * 只支持解析这个命名空间中的标签元素
     */
    public static final String BEANS_NAMESPACE_URI = "http://www.springframework.org/schema/beans";

    public static final String MULTI_VALUE_ATTRIBUTE_DELIMITERS = ",; ";

    public static final String TRUE_VALUE = "true";

    public static final String FALSE_VALUE = "false";

    public static final String DEFAULT_VALUE = "default";

    public static final String DESCRIPTION_ELEMENT = "description";

    public static final String AUTOWIRE_NO_VALUE = "no";

    public static final String AUTOWIRE_BY_NAME_VALUE = "byName";

    public static final String AUTOWIRE_BY_TYPE_VALUE = "byType";

    public static final String AUTOWIRE_CONSTRUCTOR_VALUE = "constructor";

    public static final String AUTOWIRE_AUTODETECT_VALUE = "autodetect";

    public static final String NAME_ATTRIBUTE = "name";

    public static final String BEAN_ELEMENT = "bean";

    public static final String META_ELEMENT = "meta";

    public static final String ID_ATTRIBUTE = "id";

    public static final String PARENT_ATTRIBUTE = "parent";

    public static final String CLASS_ATTRIBUTE = "class";

    public static final String ABSTRACT_ATTRIBUTE = "abstract";

    public static final String SCOPE_ATTRIBUTE = "scope";

    private static final String SINGLETON_ATTRIBUTE = "singleton";

    public static final String LAZY_INIT_ATTRIBUTE = "lazy-init";

    public static final String AUTOWIRE_ATTRIBUTE = "autowire";

    public static final String AUTOWIRE_CANDIDATE_ATTRIBUTE = "autowire-candidate";

    public static final String PRIMARY_ATTRIBUTE = "primary";

    public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";

    public static final String INIT_METHOD_ATTRIBUTE = "init-method";

    public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";

    public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";

    public static final String FACTORY_BEAN_ATTRIBUTE = "factory-bean";

    public static final String CONSTRUCTOR_ARG_ELEMENT = "constructor-arg";

    public static final String INDEX_ATTRIBUTE = "index";

    public static final String TYPE_ATTRIBUTE = "type";

    public static final String VALUE_TYPE_ATTRIBUTE = "value-type";

    public static final String KEY_TYPE_ATTRIBUTE = "key-type";

    public static final String PROPERTY_ELEMENT = "property";

    public static final String REF_ATTRIBUTE = "ref";

    public static final String VALUE_ATTRIBUTE = "value";

    public static final String LOOKUP_METHOD_ELEMENT = "lookup-method";

    public static final String REPLACED_METHOD_ELEMENT = "replaced-method";

    public static final String REPLACER_ATTRIBUTE = "replacer";

    public static final String ARG_TYPE_ELEMENT = "arg-type";

    public static final String ARG_TYPE_MATCH_ATTRIBUTE = "match";

    public static final String REF_ELEMENT = "ref";

    public static final String IDREF_ELEMENT = "idref";

    public static final String BEAN_REF_ATTRIBUTE = "bean";

    public static final String PARENT_REF_ATTRIBUTE = "parent";

    public static final String VALUE_ELEMENT = "value";

    public static final String NULL_ELEMENT = "null";

    public static final String ARRAY_ELEMENT = "array";

    public static final String LIST_ELEMENT = "list";

    public static final String SET_ELEMENT = "set";

    public static final String MAP_ELEMENT = "map";

    public static final String ENTRY_ELEMENT = "entry";

    public static final String KEY_ELEMENT = "key";

    public static final String KEY_ATTRIBUTE = "key";

    public static final String KEY_REF_ATTRIBUTE = "key-ref";

    public static final String VALUE_REF_ATTRIBUTE = "value-ref";

    public static final String PROPS_ELEMENT = "props";

    public static final String PROP_ELEMENT = "prop";

    public static final String MERGE_ATTRIBUTE = "merge";

    public static final String QUALIFIER_ELEMENT = "qualifier";

    public static final String QUALIFIER_ATTRIBUTE_ELEMENT = "attribute";

    public static final String DEFAULT_LAZY_INIT_ATTRIBUTE = "default-lazy-init";

    public static final String DEFAULT_MERGE_ATTRIBUTE = "default-merge";

    public static final String DEFAULT_AUTOWIRE_ATTRIBUTE = "default-autowire";

    public static final String DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE = "default-autowire-candidates";

    public static final String DEFAULT_INIT_METHOD_ATTRIBUTE = "default-init-method";

    public static final String DEFAULT_DESTROY_METHOD_ATTRIBUTE = "default-destroy-method";

    protected final Log logger = LogFactory.getLog(getClass());

    private final XmlReaderContext readerContext;

    private final DocumentDefaultsDefinition defaults = new DocumentDefaultsDefinition();

    private final ParseState parseState = new ParseState();

    private final Set<String> usedNames = new HashSet<>();

    public BeanDefinitionParserDelegate(XmlReaderContext readerContext) {
        Assert.notNull(readerContext, "XmlReaderContext must not be null");
        this.readerContext = readerContext;
    }

    public final XmlReaderContext getReaderContext() {
        return this.readerContext;
    }

    @Nullable
    protected Object extractSource(Element ele) {
        return this.readerContext.extractSource(ele);
    }

    protected void error(String message, Node source) {
        this.readerContext.error(message, source, this.parseState.snapshot());
    }

    protected void error(String message, Element source) {
        this.readerContext.error(message, source, this.parseState.snapshot());
    }

    protected void error(String message, Element source, Throwable cause) {
        this.readerContext.error(message, source, this.parseState.snapshot(), cause);
    }

    public void initDefaults(Element root) {
        initDefaults(root, null);
    }

    public void initDefaults(Element root, @Nullable BeanDefinitionParserDelegate parent) {
        populateDefaults(this.defaults, (parent != null ? parent.defaults : null), root);
        this.readerContext.fireDefaultsRegistered(this.defaults);
    }

    protected void populateDefaults(DocumentDefaultsDefinition defaults, @Nullable DocumentDefaultsDefinition parentDefaults, Element root) {
        String lazyInit = root.getAttribute(DEFAULT_LAZY_INIT_ATTRIBUTE);
        if (isDefaultValue(lazyInit)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
            lazyInit = (parentDefaults != null ? parentDefaults.getLazyInit() : FALSE_VALUE);
        }
        defaults.setLazyInit(lazyInit);
        String merge = root.getAttribute(DEFAULT_MERGE_ATTRIBUTE);
        if (isDefaultValue(merge)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to false.
            merge = (parentDefaults != null ? parentDefaults.getMerge() : FALSE_VALUE);
        }
        defaults.setMerge(merge);
        String autowire = root.getAttribute(DEFAULT_AUTOWIRE_ATTRIBUTE);
        if (isDefaultValue(autowire)) {
            // Potentially inherited from outer <beans> sections, otherwise falling back to 'no'.
            autowire = (parentDefaults != null ? parentDefaults.getAutowire() : AUTOWIRE_NO_VALUE);
        }
        defaults.setAutowire(autowire);
        if (root.hasAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE)) {
            defaults.setAutowireCandidates(root.getAttribute(DEFAULT_AUTOWIRE_CANDIDATES_ATTRIBUTE));
        } else if (parentDefaults != null) {
            defaults.setAutowireCandidates(parentDefaults.getAutowireCandidates());
        }
        if (root.hasAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE)) {
            defaults.setInitMethod(root.getAttribute(DEFAULT_INIT_METHOD_ATTRIBUTE));
        } else if (parentDefaults != null) {
            defaults.setInitMethod(parentDefaults.getInitMethod());
        }
        if (root.hasAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE)) {
            defaults.setDestroyMethod(root.getAttribute(DEFAULT_DESTROY_METHOD_ATTRIBUTE));
        } else if (parentDefaults != null) {
            defaults.setDestroyMethod(parentDefaults.getDestroyMethod());
        }
        defaults.setSource(this.readerContext.extractSource(root));
    }

    public DocumentDefaultsDefinition getDefaults() {
        return this.defaults;
    }

    public BeanDefinitionDefaults getBeanDefinitionDefaults() {
        BeanDefinitionDefaults bdd = new BeanDefinitionDefaults();
        bdd.setLazyInit(TRUE_VALUE.equalsIgnoreCase(this.defaults.getLazyInit()));
        bdd.setAutowireMode(getAutowireMode(DEFAULT_VALUE));
        bdd.setInitMethodName(this.defaults.getInitMethod());
        bdd.setDestroyMethodName(this.defaults.getDestroyMethod());
        return bdd;
    }

    @Nullable
    public String[] getAutowireCandidatePatterns() {
        String candidatePattern = this.defaults.getAutowireCandidates();
        return (candidatePattern != null ? StringUtils.commaDelimitedListToStringArray(candidatePattern) : null);
    }

    @Nullable
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
        return parseBeanDefinitionElement(ele, null);
    }

    /**
     * 解析id和name标签的，生成BeanDefinitionHolder返回
     */
    @Nullable
    public BeanDefinitionHolder parseBeanDefinitionElement(Element ele, @Nullable BeanDefinition containingBean) {
        String id = ele.getAttribute(ID_ATTRIBUTE);
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
        List<String> aliases = new ArrayList<>();
        if (StringUtils.hasLength(nameAttr)) {
            // 将name属性按“逗号、分号、空格” 切分，形成别名列表数组
            String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
            aliases.addAll(Arrays.asList(nameArr));
        }
        // 默认id
        String beanName = id;
        // 没有指定id
        if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
            // 用别名列表的第一个名字作为beanName
            beanName = aliases.remove(0);
            if (logger.isTraceEnabled()) {
                logger.trace("No XML 'id' specified - using '" + beanName + "' as bean name and " + aliases + " as aliases");
            }
        }
        if (containingBean == null) {
            checkNameUniqueness(beanName, aliases, ele);
        }
        // 根据<bean></bean>创建BeanDefinition
        AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
        // <bean/>解析结束
        if (beanDefinition != null) {
            // 如果没有设置id和name，此时beanName为null
            if (!StringUtils.hasText(beanName)) {
                try {
                    if (containingBean != null) {
                        beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, this.readerContext.getRegistry(), true);
                    } else {
                        // 自动生成
                        beanName = this.readerContext.generateBeanName(beanDefinition);
                        // Register an alias for the plain bean class name, if still possible,
                        // if the generator returned the class name plus a suffix.
                        // This is expected for Spring 1.2/2.0 backwards compatibility.
                        String beanClassName = beanDefinition.getBeanClassName();
                        if (beanClassName != null && beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() && !this.readerContext.getRegistry().isBeanNameInUse(beanClassName)) {
                            // 把beanClassName设为Bean的别名
                            aliases.add(beanClassName);
                        }
                    }
                    if (logger.isTraceEnabled()) {
                        logger.trace("Neither XML 'id' nor 'name' specified - " + "using generated bean name [" + beanName + "]");
                    }
                } catch (Exception ex) {
                    error(ex.getMessage(), ele);
                    return null;
                }
            }
            String[] aliasesArray = StringUtils.toStringArray(aliases);
            // 返回BeanDefinitionHolder
            return new BeanDefinitionHolder(beanDefinition, beanName, aliasesArray);
        }
        return null;
    }

    protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
        String foundName = null;
        if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) {
            foundName = beanName;
        }
        if (foundName == null) {
            foundName = CollectionUtils.findFirstMatch(this.usedNames, aliases);
        }
        if (foundName != null) {
            error("Bean name '" + foundName + "' is already used in this <beans> element", beanElement);
        }
        this.usedNames.add(beanName);
        this.usedNames.addAll(aliases);
    }

    @Nullable
    public AbstractBeanDefinition parseBeanDefinitionElement(Element ele, String beanName, @Nullable BeanDefinition containingBean) {
        this.parseState.push(new BeanEntry(beanName));
        String className = null;
        // 解析class标签
        if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
            className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
        }
        String parent = null;
        if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
            parent = ele.getAttribute(PARENT_ATTRIBUTE);
        }
        try {
            // 创建BeanDefinition，设置类信息
            AbstractBeanDefinition bd = createBeanDefinition(className, parent);
            // 设置BeanDefinition的属性
            parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
            bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));
            // 解析<meta/>
            parseMetaElements(ele, bd);
            // 解析<lookup-method/>
            parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
            // 解析<replaced-method/>
            parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
            // 解析<constructor-arg/>
            parseConstructorArgElements(ele, bd);
            // 解析<property/>
            parsePropertyElements(ele, bd);
            // 解析<qualifier/>
            parseQualifierElements(ele, bd);
            bd.setResource(this.readerContext.getResource());
            bd.setSource(extractSource(ele));
            // 最终生成BeanDefinition返回
            return bd;
        } catch (ClassNotFoundException ex) {
            error("Bean class [" + className + "] not found", ele, ex);
        } catch (NoClassDefFoundError err) {
            error("Class that bean class [" + className + "] depends on not found", ele, err);
        } catch (Throwable ex) {
            error("Unexpected failure during bean definition parsing", ele, ex);
        } finally {
            this.parseState.pop();
        }
        return null;
    }

    public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName, @Nullable BeanDefinition containingBean, AbstractBeanDefinition bd) {
        if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
            error("Old 1.x 'singleton' attribute in use - upgrade to 'scope' declaration", ele);
        } else if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
            bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
        } else if (containingBean != null) {
            // Take default from containing bean in case of an inner bean definition.
            bd.setScope(containingBean.getScope());
        }
        if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) {
            bd.setAbstract(TRUE_VALUE.equals(ele.getAttribute(ABSTRACT_ATTRIBUTE)));
        }
        String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
        if (isDefaultValue(lazyInit)) {
            lazyInit = this.defaults.getLazyInit();
        }
        bd.setLazyInit(TRUE_VALUE.equals(lazyInit));
        String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
        bd.setAutowireMode(getAutowireMode(autowire));
        if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
            bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
        }
        String autowireCandidate = ele.getAttribute(AUTOWIRE_CANDIDATE_ATTRIBUTE);
        if (isDefaultValue(autowireCandidate)) {
            String candidatePattern = this.defaults.getAutowireCandidates();
            if (candidatePattern != null) {
                String[] patterns = StringUtils.commaDelimitedListToStringArray(candidatePattern);
                bd.setAutowireCandidate(PatternMatchUtils.simpleMatch(patterns, beanName));
            }
        } else {
            bd.setAutowireCandidate(TRUE_VALUE.equals(autowireCandidate));
        }
        if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) {
            bd.setPrimary(TRUE_VALUE.equals(ele.getAttribute(PRIMARY_ATTRIBUTE)));
        }
        if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
            String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
            bd.setInitMethodName(initMethodName);
        } else if (this.defaults.getInitMethod() != null) {
            bd.setInitMethodName(this.defaults.getInitMethod());
            bd.setEnforceInitMethod(false);
        }
        if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
            String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
            bd.setDestroyMethodName(destroyMethodName);
        } else if (this.defaults.getDestroyMethod() != null) {
            bd.setDestroyMethodName(this.defaults.getDestroyMethod());
            bd.setEnforceDestroyMethod(false);
        }
        if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
            bd.setFactoryMethodName(ele.getAttribute(FACTORY_METHOD_ATTRIBUTE));
        }
        if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) {
            bd.setFactoryBeanName(ele.getAttribute(FACTORY_BEAN_ATTRIBUTE));
        }
        return bd;
    }

    protected AbstractBeanDefinition createBeanDefinition(@Nullable String className, @Nullable String parentName) throws ClassNotFoundException {
        return BeanDefinitionReaderUtils.createBeanDefinition(parentName, className, this.readerContext.getBeanClassLoader());
    }

    public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
        NodeList nl = ele.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, META_ELEMENT)) {
                Element metaElement = (Element) node;
                String key = metaElement.getAttribute(KEY_ATTRIBUTE);
                String value = metaElement.getAttribute(VALUE_ATTRIBUTE);
                BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
                attribute.setSource(extractSource(metaElement));
                attributeAccessor.addMetadataAttribute(attribute);
            }
        }
    }

    @SuppressWarnings("deprecation")
    public int getAutowireMode(String attValue) {
        String att = attValue;
        if (isDefaultValue(att)) {
            att = this.defaults.getAutowire();
        }
        int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
        if (AUTOWIRE_BY_NAME_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
        } else if (AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
        } else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
        } else if (AUTOWIRE_AUTODETECT_VALUE.equals(att)) {
            autowire = AbstractBeanDefinition.AUTOWIRE_AUTODETECT;
        }
        // Else leave default value.
        return autowire;
    }

    public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) {
                parseConstructorArgElement((Element) node, bd);
            }
        }
    }

    public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)) {
                parsePropertyElement((Element) node, bd);
            }
        }
    }

    public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ELEMENT)) {
                parseQualifierElement((Element) node, bd);
            }
        }
    }

    public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
                Element ele = (Element) node;
                String methodName = ele.getAttribute(NAME_ATTRIBUTE);
                String beanRef = ele.getAttribute(BEAN_ELEMENT);
                LookupOverride override = new LookupOverride(methodName, beanRef);
                override.setSource(extractSource(ele));
                overrides.addOverride(override);
            }
        }
    }

    public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
        NodeList nl = beanEle.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (isCandidateElement(node) && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
                Element replacedMethodEle = (Element) node;
                String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
                String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
                ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
                // Look for arg-type match elements.
                List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
                for (Element argTypeEle : argTypeEles) {
                    String match = argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE);
                    match = (StringUtils.hasText(match) ? match : DomUtils.getTextValue(argTypeEle));
                    if (StringUtils.hasText(match)) {
                        replaceOverride.addTypeIdentifier(match);
                    }
                }
                replaceOverride.setSource(extractSource(replacedMethodEle));
                overrides.addOverride(replaceOverride);
            }
        }
    }

    public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
        String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
        String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
        String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
        if (StringUtils.hasLength(indexAttr)) {
            try {
                int index = Integer.parseInt(indexAttr);
                if (index < 0) {
                    error("'index' cannot be lower than 0", ele);
                } else {
                    try {
                        this.parseState.push(new ConstructorArgumentEntry(index));
                        Object value = parsePropertyValue(ele, bd, null);
                        ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                        if (StringUtils.hasLength(typeAttr)) {
                            valueHolder.setType(typeAttr);
                        }
                        if (StringUtils.hasLength(nameAttr)) {
                            valueHolder.setName(nameAttr);
                        }
                        valueHolder.setSource(extractSource(ele));
                        if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
                            error("Ambiguous constructor-arg entries for index " + index, ele);
                        } else {
                            bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
                        }
                    } finally {
                        this.parseState.pop();
                    }
                }
            } catch (NumberFormatException ex) {
                error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
            }
        } else {
            try {
                this.parseState.push(new ConstructorArgumentEntry());
                Object value = parsePropertyValue(ele, bd, null);
                ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
                if (StringUtils.hasLength(typeAttr)) {
                    valueHolder.setType(typeAttr);
                }
                if (StringUtils.hasLength(nameAttr)) {
                    valueHolder.setName(nameAttr);
                }
                valueHolder.setSource(extractSource(ele));
                bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
            } finally {
                this.parseState.pop();
            }
        }
    }

    public void parsePropertyElement(Element ele, BeanDefinition bd) {
        String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
        if (!StringUtils.hasLength(propertyName)) {
            error("Tag 'property' must have a 'name' attribute", ele);
            return;
        }
        this.parseState.push(new PropertyEntry(propertyName));
        try {
            if (bd.getPropertyValues().contains(propertyName)) {
                error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
                return;
            }
            Object val = parsePropertyValue(ele, bd, propertyName);
            PropertyValue pv = new PropertyValue(propertyName, val);
            parseMetaElements(ele, pv);
            pv.setSource(extractSource(ele));
            bd.getPropertyValues().addPropertyValue(pv);
        } finally {
            this.parseState.pop();
        }
    }

    public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
        String typeName = ele.getAttribute(TYPE_ATTRIBUTE);
        if (!StringUtils.hasLength(typeName)) {
            error("Tag 'qualifier' must have a 'type' attribute", ele);
            return;
        }
        this.parseState.push(new QualifierEntry(typeName));
        try {
            AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
            qualifier.setSource(extractSource(ele));
            String value = ele.getAttribute(VALUE_ATTRIBUTE);
            if (StringUtils.hasLength(value)) {
                qualifier.setAttribute(AutowireCandidateQualifier.VALUE_KEY, value);
            }
            NodeList nl = ele.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node node = nl.item(i);
                if (isCandidateElement(node) && nodeNameEquals(node, QUALIFIER_ATTRIBUTE_ELEMENT)) {
                    Element attributeEle = (Element) node;
                    String attributeName = attributeEle.getAttribute(KEY_ATTRIBUTE);
                    String attributeValue = attributeEle.getAttribute(VALUE_ATTRIBUTE);
                    if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
                        BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
                        attribute.setSource(extractSource(attributeEle));
                        qualifier.addMetadataAttribute(attribute);
                    } else {
                        error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
                        return;
                    }
                }
            }
            bd.addQualifier(qualifier);
        } finally {
            this.parseState.pop();
        }
    }

    @Nullable
    public Object parsePropertyValue(Element ele, BeanDefinition bd, @Nullable String propertyName) {
        String elementName = (propertyName != null ? "<property> element for property '" + propertyName + "'" : "<constructor-arg> element");
        // Should only have one child element: ref, value, list, etc.
        NodeList nl = ele.getChildNodes();
        Element subElement = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) && !nodeNameEquals(node, META_ELEMENT)) {
                // Child element is what we're looking for.
                if (subElement != null) {
                    error(elementName + " must not contain more than one sub-element", ele);
                } else {
                    subElement = (Element) node;
                }
            }
        }
        boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
        boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
        if ((hasRefAttribute && hasValueAttribute) || ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
            error(elementName + " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
        }
        if (hasRefAttribute) {
            String refName = ele.getAttribute(REF_ATTRIBUTE);
            if (!StringUtils.hasText(refName)) {
                error(elementName + " contains empty 'ref' attribute", ele);
            }
            RuntimeBeanReference ref = new RuntimeBeanReference(refName);
            ref.setSource(extractSource(ele));
            return ref;
        } else if (hasValueAttribute) {
            TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
            valueHolder.setSource(extractSource(ele));
            return valueHolder;
        } else if (subElement != null) {
            return parsePropertySubElement(subElement, bd);
        } else {
            // Neither child element nor "ref" or "value" attribute found.
            error(elementName + " must specify a ref or value", ele);
            return null;
        }
    }

    @Nullable
    public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd) {
        return parsePropertySubElement(ele, bd, null);
    }

    @Nullable
    public Object parsePropertySubElement(Element ele, @Nullable BeanDefinition bd, @Nullable String defaultValueType) {
        if (!isDefaultNamespace(ele)) {
            return parseNestedCustomElement(ele, bd);
        } else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
            BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
            if (nestedBd != null) {
                nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
            }
            return nestedBd;
        } else if (nodeNameEquals(ele, REF_ELEMENT)) {
            // A generic reference to any name of any bean.
            String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
            boolean toParent = false;
            if (!StringUtils.hasLength(refName)) {
                // A reference to the id of another bean in a parent context.
                refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
                toParent = true;
                if (!StringUtils.hasLength(refName)) {
                    error("'bean' or 'parent' is required for <ref> element", ele);
                    return null;
                }
            }
            if (!StringUtils.hasText(refName)) {
                error("<ref> element contains empty target attribute", ele);
                return null;
            }
            RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
            ref.setSource(extractSource(ele));
            return ref;
        } else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
            return parseIdRefElement(ele);
        } else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
            return parseValueElement(ele, defaultValueType);
        } else if (nodeNameEquals(ele, NULL_ELEMENT)) {
            // It's a distinguished null value. Let's wrap it in a TypedStringValue
            // object in order to preserve the source location.
            TypedStringValue nullHolder = new TypedStringValue(null);
            nullHolder.setSource(extractSource(ele));
            return nullHolder;
        } else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
            return parseArrayElement(ele, bd);
        } else if (nodeNameEquals(ele, LIST_ELEMENT)) {
            return parseListElement(ele, bd);
        } else if (nodeNameEquals(ele, SET_ELEMENT)) {
            return parseSetElement(ele, bd);
        } else if (nodeNameEquals(ele, MAP_ELEMENT)) {
            return parseMapElement(ele, bd);
        } else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
            return parsePropsElement(ele);
        } else {
            error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
            return null;
        }
    }

    @Nullable
    public Object parseIdRefElement(Element ele) {
        // A generic reference to any name of any bean.
        String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
        if (!StringUtils.hasLength(refName)) {
            error("'bean' is required for <idref> element", ele);
            return null;
        }
        if (!StringUtils.hasText(refName)) {
            error("<idref> element contains empty target attribute", ele);
            return null;
        }
        RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
        ref.setSource(extractSource(ele));
        return ref;
    }

    public Object parseValueElement(Element ele, @Nullable String defaultTypeName) {
        // It's a literal value.
        String value = DomUtils.getTextValue(ele);
        String specifiedTypeName = ele.getAttribute(TYPE_ATTRIBUTE);
        String typeName = specifiedTypeName;
        if (!StringUtils.hasText(typeName)) {
            typeName = defaultTypeName;
        }
        try {
            TypedStringValue typedValue = buildTypedStringValue(value, typeName);
            typedValue.setSource(extractSource(ele));
            typedValue.setSpecifiedTypeName(specifiedTypeName);
            return typedValue;
        } catch (ClassNotFoundException ex) {
            error("Type class [" + typeName + "] not found for <value> element", ele, ex);
            return value;
        }
    }

    protected TypedStringValue buildTypedStringValue(String value, @Nullable String targetTypeName) throws ClassNotFoundException {
        ClassLoader classLoader = this.readerContext.getBeanClassLoader();
        TypedStringValue typedValue;
        if (!StringUtils.hasText(targetTypeName)) {
            typedValue = new TypedStringValue(value);
        } else if (classLoader != null) {
            Class<?> targetType = ClassUtils.forName(targetTypeName, classLoader);
            typedValue = new TypedStringValue(value, targetType);
        } else {
            typedValue = new TypedStringValue(value, targetTypeName);
        }
        return typedValue;
    }

    public Object parseArrayElement(Element arrayEle, @Nullable BeanDefinition bd) {
        String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
        NodeList nl = arrayEle.getChildNodes();
        ManagedArray target = new ManagedArray(elementType, nl.getLength());
        target.setSource(extractSource(arrayEle));
        target.setElementTypeName(elementType);
        target.setMergeEnabled(parseMergeAttribute(arrayEle));
        parseCollectionElements(nl, target, bd, elementType);
        return target;
    }

    public List<Object> parseListElement(Element collectionEle, @Nullable BeanDefinition bd) {
        String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
        NodeList nl = collectionEle.getChildNodes();
        ManagedList<Object> target = new ManagedList<>(nl.getLength());
        target.setSource(extractSource(collectionEle));
        target.setElementTypeName(defaultElementType);
        target.setMergeEnabled(parseMergeAttribute(collectionEle));
        parseCollectionElements(nl, target, bd, defaultElementType);
        return target;
    }

    public Set<Object> parseSetElement(Element collectionEle, @Nullable BeanDefinition bd) {
        String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
        NodeList nl = collectionEle.getChildNodes();
        ManagedSet<Object> target = new ManagedSet<>(nl.getLength());
        target.setSource(extractSource(collectionEle));
        target.setElementTypeName(defaultElementType);
        target.setMergeEnabled(parseMergeAttribute(collectionEle));
        parseCollectionElements(nl, target, bd, defaultElementType);
        return target;
    }

    protected void parseCollectionElements(NodeList elementNodes, Collection<Object> target, @Nullable BeanDefinition bd, String defaultElementType) {
        for (int i = 0; i < elementNodes.getLength(); i++) {
            Node node = elementNodes.item(i);
            if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
                target.add(parsePropertySubElement((Element) node, bd, defaultElementType));
            }
        }
    }

    public Map<Object, Object> parseMapElement(Element mapEle, @Nullable BeanDefinition bd) {
        String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE);
        String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
        List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
        ManagedMap<Object, Object> map = new ManagedMap<>(entryEles.size());
        map.setSource(extractSource(mapEle));
        map.setKeyTypeName(defaultKeyType);
        map.setValueTypeName(defaultValueType);
        map.setMergeEnabled(parseMergeAttribute(mapEle));
        for (Element entryEle : entryEles) {
            // Should only have one value child element: ref, value, list, etc.
            // Optionally, there might be a key child element.
            NodeList entrySubNodes = entryEle.getChildNodes();
            Element keyEle = null;
            Element valueEle = null;
            for (int j = 0; j < entrySubNodes.getLength(); j++) {
                Node node = entrySubNodes.item(j);
                if (node instanceof Element) {
                    Element candidateEle = (Element) node;
                    if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
                        if (keyEle != null) {
                            error("<entry> element is only allowed to contain one <key> sub-element", entryEle);
                        } else {
                            keyEle = candidateEle;
                        }
                    } else {
                        // Child element is what we're looking for.
                        if (nodeNameEquals(candidateEle, DESCRIPTION_ELEMENT)) {
                            // the element is a <description> -> ignore it
                        } else if (valueEle != null) {
                            error("<entry> element must not contain more than one value sub-element", entryEle);
                        } else {
                            valueEle = candidateEle;
                        }
                    }
                }
            }
            // Extract key from attribute or sub-element.
            Object key = null;
            boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE);
            boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE);
            if ((hasKeyAttribute && hasKeyRefAttribute) || (hasKeyAttribute || hasKeyRefAttribute) && keyEle != null) {
                error("<entry> element is only allowed to contain either " + "a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
            }
            if (hasKeyAttribute) {
                key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
            } else if (hasKeyRefAttribute) {
                String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
                if (!StringUtils.hasText(refName)) {
                    error("<entry> element contains empty 'key-ref' attribute", entryEle);
                }
                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
                ref.setSource(extractSource(entryEle));
                key = ref;
            } else if (keyEle != null) {
                key = parseKeyElement(keyEle, bd, defaultKeyType);
            } else {
                error("<entry> element must specify a key", entryEle);
            }
            // Extract value from attribute or sub-element.
            Object value = null;
            boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE);
            boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE);
            boolean hasValueTypeAttribute = entryEle.hasAttribute(VALUE_TYPE_ATTRIBUTE);
            if ((hasValueAttribute && hasValueRefAttribute) || (hasValueAttribute || hasValueRefAttribute) && valueEle != null) {
                error("<entry> element is only allowed to contain either " + "'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
            }
            if ((hasValueTypeAttribute && hasValueRefAttribute) || (hasValueTypeAttribute && !hasValueAttribute) || (hasValueTypeAttribute && valueEle != null)) {
                error("<entry> element is only allowed to contain a 'value-type' " + "attribute when it has a 'value' attribute", entryEle);
            }
            if (hasValueAttribute) {
                String valueType = entryEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
                if (!StringUtils.hasText(valueType)) {
                    valueType = defaultValueType;
                }
                value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), valueType, entryEle);
            } else if (hasValueRefAttribute) {
                String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
                if (!StringUtils.hasText(refName)) {
                    error("<entry> element contains empty 'value-ref' attribute", entryEle);
                }
                RuntimeBeanReference ref = new RuntimeBeanReference(refName);
                ref.setSource(extractSource(entryEle));
                value = ref;
            } else if (valueEle != null) {
                value = parsePropertySubElement(valueEle, bd, defaultValueType);
            } else {
                error("<entry> element must specify a value", entryEle);
            }
            // Add final key and value to the Map.
            map.put(key, value);
        }
        return map;
    }

    protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
        try {
            TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
            typedValue.setSource(extractSource(entryEle));
            return typedValue;
        } catch (ClassNotFoundException ex) {
            error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle, ex);
            return value;
        }
    }

    @Nullable
    protected Object parseKeyElement(Element keyEle, @Nullable BeanDefinition bd, String defaultKeyTypeName) {
        NodeList nl = keyEle.getChildNodes();
        Element subElement = null;
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                // Child element is what we're looking for.
                if (subElement != null) {
                    error("<key> element must not contain more than one value sub-element", keyEle);
                } else {
                    subElement = (Element) node;
                }
            }
        }
        if (subElement == null) {
            return null;
        }
        return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
    }

    public Properties parsePropsElement(Element propsEle) {
        ManagedProperties props = new ManagedProperties();
        props.setSource(extractSource(propsEle));
        props.setMergeEnabled(parseMergeAttribute(propsEle));
        List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
        for (Element propEle : propEles) {
            String key = propEle.getAttribute(KEY_ATTRIBUTE);
            // Trim the text value to avoid unwanted whitespace
            // caused by typical XML formatting.
            String value = DomUtils.getTextValue(propEle).trim();
            TypedStringValue keyHolder = new TypedStringValue(key);
            keyHolder.setSource(extractSource(propEle));
            TypedStringValue valueHolder = new TypedStringValue(value);
            valueHolder.setSource(extractSource(propEle));
            props.put(keyHolder, valueHolder);
        }
        return props;
    }

    public boolean parseMergeAttribute(Element collectionElement) {
        String value = collectionElement.getAttribute(MERGE_ATTRIBUTE);
        if (isDefaultValue(value)) {
            value = this.defaults.getMerge();
        }
        return TRUE_VALUE.equals(value);
    }

    @Nullable
    public BeanDefinition parseCustomElement(Element ele) {
        return parseCustomElement(ele, null);
    }

    @Nullable
    public BeanDefinition parseCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
        // 获取xml配置文件中的命名空间，如 http://www.springframework.org/schema/aop
        String namespaceUri = getNamespaceURI(ele);
        if (namespaceUri == null) {
            return null;
        }
        // 根据命名空间找到命名空间处理类，如AopNamespaceHandler
        NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
        if (handler == null) {
            error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", ele);
            return null;
        }
        // 解析命名空间支持的标签
        return handler.parse(ele, new ParserContext(this.readerContext, this, containingBd));
    }

    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder definitionHolder) {
        return decorateBeanDefinitionIfRequired(ele, definitionHolder, null);
    }

    public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele, BeanDefinitionHolder definitionHolder, @Nullable BeanDefinition containingBd) {
        BeanDefinitionHolder finalDefinition = definitionHolder;
        // Decorate based on custom attributes first.
        NamedNodeMap attributes = ele.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
        }
        // Decorate based on custom nested elements.
        NodeList children = ele.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
            }
        }
        return finalDefinition;
    }

    public BeanDefinitionHolder decorateIfRequired(Node node, BeanDefinitionHolder originalDef, @Nullable BeanDefinition containingBd) {
        String namespaceUri = getNamespaceURI(node);
        if (namespaceUri != null && !isDefaultNamespace(namespaceUri)) {
            NamespaceHandler handler = this.readerContext.getNamespaceHandlerResolver().resolve(namespaceUri);
            if (handler != null) {
                BeanDefinitionHolder decorated = handler.decorate(node, originalDef, new ParserContext(this.readerContext, this, containingBd));
                if (decorated != null) {
                    return decorated;
                }
            } else if (namespaceUri.startsWith("http://www.springframework.org/")) {
                error("Unable to locate Spring NamespaceHandler for XML schema namespace [" + namespaceUri + "]", node);
            } else {
                // A custom namespace, not to be handled by Spring - maybe "xml:...".
                if (logger.isDebugEnabled()) {
                    logger.debug("No Spring NamespaceHandler found for XML schema namespace [" + namespaceUri + "]");
                }
            }
        }
        return originalDef;
    }

    @Nullable
    private BeanDefinitionHolder parseNestedCustomElement(Element ele, @Nullable BeanDefinition containingBd) {
        BeanDefinition innerDefinition = parseCustomElement(ele, containingBd);
        if (innerDefinition == null) {
            error("Incorrect usage of element '" + ele.getNodeName() + "' in a nested manner. " + "This tag cannot be used nested inside <property>.", ele);
            return null;
        }
        String id = ele.getNodeName() + BeanDefinitionReaderUtils.GENERATED_BEAN_NAME_SEPARATOR + ObjectUtils.getIdentityHexString(innerDefinition);
        if (logger.isTraceEnabled()) {
            logger.trace("Using generated bean name [" + id + "] for nested custom element '" + ele.getNodeName() + "'");
        }
        return new BeanDefinitionHolder(innerDefinition, id);
    }

    @Nullable
    public String getNamespaceURI(Node node) {
        return node.getNamespaceURI();
    }

    public String getLocalName(Node node) {
        return node.getLocalName();
    }

    public boolean nodeNameEquals(Node node, String desiredName) {
        return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
    }

    public boolean isDefaultNamespace(@Nullable String namespaceUri) {
        return (!StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri));
    }

    public boolean isDefaultNamespace(Node node) {
        return isDefaultNamespace(getNamespaceURI(node));
    }

    private boolean isDefaultValue(String value) {
        return (DEFAULT_VALUE.equals(value) || "".equals(value));
    }

    private boolean isCandidateElement(Node node) {
        return (node instanceof Element && (isDefaultNamespace(node) || !isDefaultNamespace(node.getParentNode())));
    }

}
