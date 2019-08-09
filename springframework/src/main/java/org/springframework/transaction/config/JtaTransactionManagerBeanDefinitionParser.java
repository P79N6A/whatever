package org.springframework.transaction.config;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class JtaTransactionManagerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    @Override
    protected String getBeanClassName(Element element) {
        return JtaTransactionManagerFactoryBean.resolveJtaTransactionManagerClassName();
    }

    @Override
    protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext) {
        return TxNamespaceHandler.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME;
    }

}
