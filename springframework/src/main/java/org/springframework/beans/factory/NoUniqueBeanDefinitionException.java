package org.springframework.beans.factory;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("serial")
public class NoUniqueBeanDefinitionException extends NoSuchBeanDefinitionException {

    private final int numberOfBeansFound;

    @Nullable
    private final Collection<String> beanNamesFound;

    public NoUniqueBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
        super(type, message);
        this.numberOfBeansFound = numberOfBeansFound;
        this.beanNamesFound = null;
    }

    public NoUniqueBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
        super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " + StringUtils.collectionToCommaDelimitedString(beanNamesFound));
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }

    public NoUniqueBeanDefinitionException(Class<?> type, String... beanNamesFound) {
        this(type, Arrays.asList(beanNamesFound));
    }

    public NoUniqueBeanDefinitionException(ResolvableType type, Collection<String> beanNamesFound) {
        super(type, "expected single matching bean but found " + beanNamesFound.size() + ": " + StringUtils.collectionToCommaDelimitedString(beanNamesFound));
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }

    public NoUniqueBeanDefinitionException(ResolvableType type, String... beanNamesFound) {
        this(type, Arrays.asList(beanNamesFound));
    }

    @Override
    public int getNumberOfBeansFound() {
        return this.numberOfBeansFound;
    }

    @Nullable
    public Collection<String> getBeanNamesFound() {
        return this.beanNamesFound;
    }

}
