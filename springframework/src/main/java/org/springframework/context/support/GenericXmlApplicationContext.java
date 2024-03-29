package org.springframework.context.support;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class GenericXmlApplicationContext extends GenericApplicationContext {

    private final XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this);

    public GenericXmlApplicationContext() {
    }

    public GenericXmlApplicationContext(Resource... resources) {
        load(resources);
        refresh();
    }

    public GenericXmlApplicationContext(String... resourceLocations) {
        load(resourceLocations);
        refresh();
    }

    public GenericXmlApplicationContext(Class<?> relativeClass, String... resourceNames) {
        load(relativeClass, resourceNames);
        refresh();
    }

    public final XmlBeanDefinitionReader getReader() {
        return this.reader;
    }

    public void setValidating(boolean validating) {
        this.reader.setValidating(validating);
    }

    @Override
    public void setEnvironment(ConfigurableEnvironment environment) {
        super.setEnvironment(environment);
        this.reader.setEnvironment(getEnvironment());
    }
    //---------------------------------------------------------------------
    // Convenient methods for loading XML bean definition files
    //---------------------------------------------------------------------

    public void load(Resource... resources) {
        this.reader.loadBeanDefinitions(resources);
    }

    public void load(String... resourceLocations) {
        this.reader.loadBeanDefinitions(resourceLocations);
    }

    public void load(Class<?> relativeClass, String... resourceNames) {
        Resource[] resources = new Resource[resourceNames.length];
        for (int i = 0; i < resourceNames.length; i++) {
            resources[i] = new ClassPathResource(resourceNames[i], relativeClass);
        }
        this.load(resources);
    }

}
