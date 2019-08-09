package org.springframework.beans.factory.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.lang.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 配置如<aop:config />
 * <p>
 * 引入这个aop所在的命名空间
 * <p>
 * <?xml version="1.0" encoding="UTF-8"?>
 * <beans xmlns="http://www.springframework.org/schema/beans"
 * xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 * xmlns:aop="http://www.springframework.org/schema/aop"
 * xmlns:context="http://www.springframework.org/schema/context"
 * xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
 * http://www.springframework.org/schema/aop http://www.springframework.org/schema/aop/spring-aop.xsd
 * http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd />
 * <p>
 * spring.handlers
 * http\://www.springframework.org/schema/aop=org.springframework.aop.config.AopNamespaceHandler
 * <p>
 * spring.schemas
 * url和本地xsd标签解析文件的关系
 * http\://www.springframework.org/schema/aop/spring-aop-2.0.xsd=org/springframework/aop/config/spring-aop-2.0.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-2.5.xsd=org/springframework/aop/config/spring-aop-2.5.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-3.0.xsd=org/springframework/aop/config/spring-aop-3.0.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-3.1.xsd=org/springframework/aop/config/spring-aop-3.1.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-3.2.xsd=org/springframework/aop/config/spring-aop-3.2.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-4.0.xsd=org/springframework/aop/config/spring-aop-4.0.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-4.1.xsd=org/springframework/aop/config/spring-aop-4.1.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-4.2.xsd=org/springframework/aop/config/spring-aop-4.2.xsd
 * http\://www.springframework.org/schema/aop/spring-aop-4.3.xsd=org/springframework/aop/config/spring-aop-4.3.xsd
 * http\://www.springframework.org/schema/aop/spring-aop.xsd=org/springframework/aop/config/spring-aop-4.3.xsd
 */
public interface NamespaceHandler {

    void init();

    @Nullable
    BeanDefinition parse(Element element, ParserContext parserContext);

    @Nullable
    BeanDefinitionHolder decorate(Node source, BeanDefinitionHolder definition, ParserContext parserContext);

}
