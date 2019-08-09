package org.springframework.http.converter.json;

import com.fasterxml.jackson.annotation.ObjectIdGenerator;
import com.fasterxml.jackson.annotation.ObjectIdResolver;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.ser.VirtualBeanPropertyWriter;
import com.fasterxml.jackson.databind.util.Converter;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

public class SpringHandlerInstantiator extends HandlerInstantiator {

    private final AutowireCapableBeanFactory beanFactory;

    public SpringHandlerInstantiator(AutowireCapableBeanFactory beanFactory) {
        Assert.notNull(beanFactory, "BeanFactory must not be null");
        this.beanFactory = beanFactory;
    }

    @Override
    public JsonDeserializer<?> deserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> implClass) {
        return (JsonDeserializer<?>) this.beanFactory.createBean(implClass);
    }

    @Override
    public KeyDeserializer keyDeserializerInstance(DeserializationConfig config, Annotated annotated, Class<?> implClass) {
        return (KeyDeserializer) this.beanFactory.createBean(implClass);
    }

    @Override
    public JsonSerializer<?> serializerInstance(SerializationConfig config, Annotated annotated, Class<?> implClass) {
        return (JsonSerializer<?>) this.beanFactory.createBean(implClass);
    }

    @Override
    public TypeResolverBuilder<?> typeResolverBuilderInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return (TypeResolverBuilder<?>) this.beanFactory.createBean(implClass);
    }

    @Override
    public TypeIdResolver typeIdResolverInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return (TypeIdResolver) this.beanFactory.createBean(implClass);
    }

    @Override
    public ValueInstantiator valueInstantiatorInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return (ValueInstantiator) this.beanFactory.createBean(implClass);
    }

    @Override
    public ObjectIdGenerator<?> objectIdGeneratorInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return (ObjectIdGenerator<?>) this.beanFactory.createBean(implClass);
    }

    @Override
    public ObjectIdResolver resolverIdGeneratorInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return (ObjectIdResolver) this.beanFactory.createBean(implClass);
    }

    @Override
    public PropertyNamingStrategy namingStrategyInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return (PropertyNamingStrategy) this.beanFactory.createBean(implClass);
    }

    @Override
    public Converter<?, ?> converterInstance(MapperConfig<?> config, Annotated annotated, Class<?> implClass) {
        return (Converter<?, ?>) this.beanFactory.createBean(implClass);
    }

    @Override
    public VirtualBeanPropertyWriter virtualPropertyWriterInstance(MapperConfig<?> config, Class<?> implClass) {
        return (VirtualBeanPropertyWriter) this.beanFactory.createBean(implClass);
    }

}
