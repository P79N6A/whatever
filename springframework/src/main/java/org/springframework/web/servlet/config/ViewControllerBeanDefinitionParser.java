package org.springframework.web.servlet.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;
import org.w3c.dom.Element;

import java.util.Map;

class ViewControllerBeanDefinitionParser implements BeanDefinitionParser {

    private static final String HANDLER_MAPPING_BEAN_NAME = "org.springframework.web.servlet.config.viewControllerHandlerMapping";

    @Override
    @SuppressWarnings("unchecked")
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        Object source = parserContext.extractSource(element);
        // Register SimpleUrlHandlerMapping for view controllers
        BeanDefinition hm = registerHandlerMapping(parserContext, source);
        // Ensure BeanNameUrlHandlerMapping (SPR-8289) and default HandlerAdapters are not "turned off"
        MvcNamespaceUtils.registerDefaultComponents(parserContext, source);
        // Create view controller bean definition
        RootBeanDefinition controller = new RootBeanDefinition(ParameterizableViewController.class);
        controller.setSource(source);
        HttpStatus statusCode = null;
        if (element.hasAttribute("status-code")) {
            int statusValue = Integer.parseInt(element.getAttribute("status-code"));
            statusCode = HttpStatus.valueOf(statusValue);
        }
        String name = element.getLocalName();
        if (name.equals("view-controller")) {
            if (element.hasAttribute("view-name")) {
                controller.getPropertyValues().add("viewName", element.getAttribute("view-name"));
            }
            if (statusCode != null) {
                controller.getPropertyValues().add("statusCode", statusCode);
            }
        } else if (name.equals("redirect-view-controller")) {
            controller.getPropertyValues().add("view", getRedirectView(element, statusCode, source));
        } else if (name.equals("status-controller")) {
            controller.getPropertyValues().add("statusCode", statusCode);
            controller.getPropertyValues().add("statusOnly", true);
        } else {
            // Should never happen...
            throw new IllegalStateException("Unexpected tag name: " + name);
        }
        Map<String, BeanDefinition> urlMap = (Map<String, BeanDefinition>) hm.getPropertyValues().get("urlMap");
        if (urlMap == null) {
            urlMap = new ManagedMap<>();
            hm.getPropertyValues().add("urlMap", urlMap);
        }
        urlMap.put(element.getAttribute("path"), controller);
        return null;
    }

    private BeanDefinition registerHandlerMapping(ParserContext context, @Nullable Object source) {
        if (context.getRegistry().containsBeanDefinition(HANDLER_MAPPING_BEAN_NAME)) {
            return context.getRegistry().getBeanDefinition(HANDLER_MAPPING_BEAN_NAME);
        }
        RootBeanDefinition beanDef = new RootBeanDefinition(SimpleUrlHandlerMapping.class);
        beanDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        context.getRegistry().registerBeanDefinition(HANDLER_MAPPING_BEAN_NAME, beanDef);
        context.registerComponent(new BeanComponentDefinition(beanDef, HANDLER_MAPPING_BEAN_NAME));
        beanDef.setSource(source);
        beanDef.getPropertyValues().add("order", "1");
        beanDef.getPropertyValues().add("pathMatcher", MvcNamespaceUtils.registerPathMatcher(null, context, source));
        beanDef.getPropertyValues().add("urlPathHelper", MvcNamespaceUtils.registerUrlPathHelper(null, context, source));
        RuntimeBeanReference corsConfigurationsRef = MvcNamespaceUtils.registerCorsConfigurations(null, context, source);
        beanDef.getPropertyValues().add("corsConfigurations", corsConfigurationsRef);
        return beanDef;
    }

    private RootBeanDefinition getRedirectView(Element element, @Nullable HttpStatus status, @Nullable Object source) {
        RootBeanDefinition redirectView = new RootBeanDefinition(RedirectView.class);
        redirectView.setSource(source);
        redirectView.getConstructorArgumentValues().addIndexedArgumentValue(0, element.getAttribute("redirect-url"));
        if (status != null) {
            redirectView.getPropertyValues().add("statusCode", status);
        }
        if (element.hasAttribute("context-relative")) {
            redirectView.getPropertyValues().add("contextRelative", element.getAttribute("context-relative"));
        } else {
            redirectView.getPropertyValues().add("contextRelative", true);
        }
        if (element.hasAttribute("keep-query-params")) {
            redirectView.getPropertyValues().add("propagateQueryParams", element.getAttribute("keep-query-params"));
        }
        return redirectView;
    }

}
