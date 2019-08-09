package org.springframework.beans.factory.parsing;

public class BeanEntry implements ParseState.Entry {

    private String beanDefinitionName;

    public BeanEntry(String beanDefinitionName) {
        this.beanDefinitionName = beanDefinitionName;
    }

    @Override
    public String toString() {
        return "Bean '" + this.beanDefinitionName + "'";
    }

}
