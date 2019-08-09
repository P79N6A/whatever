package org.springframework.boot.context;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;
import java.util.Collection;

public class TypeExcludeFilter implements TypeFilter, BeanFactoryAware {

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws IOException {
        if (this.beanFactory instanceof ListableBeanFactory && getClass() == TypeExcludeFilter.class) {
            Collection<TypeExcludeFilter> delegates = ((ListableBeanFactory) this.beanFactory).getBeansOfType(TypeExcludeFilter.class).values();
            for (TypeExcludeFilter delegate : delegates) {
                if (delegate.match(metadataReader, metadataReaderFactory)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        throw new IllegalStateException("TypeExcludeFilter " + getClass() + " has not implemented equals");
    }

    @Override
    public int hashCode() {
        throw new IllegalStateException("TypeExcludeFilter " + getClass() + " has not implemented hashCode");
    }

}
