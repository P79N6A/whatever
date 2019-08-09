package org.springframework.web.servlet.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;
import org.w3c.dom.Element;

import java.util.Map;

class DefaultServletHandlerBeanDefinitionParser implements BeanDefinitionParser {

    @Override
    @Nullable
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        Object source = parserContext.extractSource(element);
        String defaultServletName = element.getAttribute("default-servlet-name");
        RootBeanDefinition defaultServletHandlerDef = new RootBeanDefinition(DefaultServletHttpRequestHandler.class);
        defaultServletHandlerDef.setSource(source);
        defaultServletHandlerDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        if (StringUtils.hasText(defaultServletName)) {
            defaultServletHandlerDef.getPropertyValues().add("defaultServletName", defaultServletName);
        }
        String defaultServletHandlerName = parserContext.getReaderContext().generateBeanName(defaultServletHandlerDef);
        parserContext.getRegistry().registerBeanDefinition(defaultServletHandlerName, defaultServletHandlerDef);
        parserContext.registerComponent(new BeanComponentDefinition(defaultServletHandlerDef, defaultServletHandlerName));
        Map<String, String> urlMap = new ManagedMap<>();
        urlMap.put("/**", defaultServletHandlerName);
        RootBeanDefinition handlerMappingDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
        handlerMappingDef.setSource(source);
        handlerMappingDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        handlerMappingDef.getPropertyValues().add("urlMap", urlMap);
        String handlerMappingBeanName = parserContext.getReaderContext().generateBeanName(handlerMappingDef);
        parserContext.getRegistry().registerBeanDefinition(handlerMappingBeanName, handlerMappingDef);
        parserContext.registerComponent(new BeanComponentDefinition(handlerMappingDef, handlerMappingBeanName));
        // Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
        MvcNamespaceUtils.registerDefaultComponents(parserContext, source);
        return null;
    }

}
