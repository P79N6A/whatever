package org.springframework.beans.factory.config;

import org.springframework.beans.factory.*;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

public class FieldRetrievingFactoryBean implements FactoryBean<Object>, BeanNameAware, BeanClassLoaderAware, InitializingBean {

    @Nullable
    private Class<?> targetClass;

    @Nullable
    private Object targetObject;

    @Nullable
    private String targetField;

    @Nullable
    private String staticField;

    @Nullable
    private String beanName;

    @Nullable
    private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

    // the field we will retrieve
    @Nullable
    private Field fieldObject;

    public void setTargetClass(@Nullable Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Nullable
    public Class<?> getTargetClass() {
        return this.targetClass;
    }

    public void setTargetObject(@Nullable Object targetObject) {
        this.targetObject = targetObject;
    }

    @Nullable
    public Object getTargetObject() {
        return this.targetObject;
    }

    public void setTargetField(@Nullable String targetField) {
        this.targetField = (targetField != null ? StringUtils.trimAllWhitespace(targetField) : null);
    }

    @Nullable
    public String getTargetField() {
        return this.targetField;
    }

    public void setStaticField(String staticField) {
        this.staticField = StringUtils.trimAllWhitespace(staticField);
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = StringUtils.trimAllWhitespace(BeanFactoryUtils.originalBeanName(beanName));
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.beanClassLoader = classLoader;
    }

    @Override
    public void afterPropertiesSet() throws ClassNotFoundException, NoSuchFieldException {
        if (this.targetClass != null && this.targetObject != null) {
            throw new IllegalArgumentException("Specify either targetClass or targetObject, not both");
        }
        if (this.targetClass == null && this.targetObject == null) {
            if (this.targetField != null) {
                throw new IllegalArgumentException("Specify targetClass or targetObject in combination with targetField");
            }
            // If no other property specified, consider bean name as static field expression.
            if (this.staticField == null) {
                this.staticField = this.beanName;
                Assert.state(this.staticField != null, "No target field specified");
            }
            // Try to parse static field into class and field.
            int lastDotIndex = this.staticField.lastIndexOf('.');
            if (lastDotIndex == -1 || lastDotIndex == this.staticField.length()) {
                throw new IllegalArgumentException("staticField must be a fully qualified class plus static field name: " + "e.g. 'example.MyExampleClass.MY_EXAMPLE_FIELD'");
            }
            String className = this.staticField.substring(0, lastDotIndex);
            String fieldName = this.staticField.substring(lastDotIndex + 1);
            this.targetClass = ClassUtils.forName(className, this.beanClassLoader);
            this.targetField = fieldName;
        } else if (this.targetField == null) {
            // Either targetClass or targetObject specified.
            throw new IllegalArgumentException("targetField is required");
        }
        // Try to get the exact method first.
        Class<?> targetClass = (this.targetObject != null ? this.targetObject.getClass() : this.targetClass);
        this.fieldObject = targetClass.getField(this.targetField);
    }

    @Override
    @Nullable
    public Object getObject() throws IllegalAccessException {
        if (this.fieldObject == null) {
            throw new FactoryBeanNotInitializedException();
        }
        ReflectionUtils.makeAccessible(this.fieldObject);
        if (this.targetObject != null) {
            // instance field
            return this.fieldObject.get(this.targetObject);
        } else {
            // class field
            return this.fieldObject.get(null);
        }
    }

    @Override
    public Class<?> getObjectType() {
        return (this.fieldObject != null ? this.fieldObject.getType() : null);
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

}
