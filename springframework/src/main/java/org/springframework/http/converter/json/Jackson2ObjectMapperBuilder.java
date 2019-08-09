package org.springframework.http.converter.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.logging.Log;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.KotlinDetector;
import org.springframework.http.HttpLogging;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.util.xml.StaxUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Jackson2ObjectMapperBuilder {

    private static volatile boolean kotlinWarningLogged = false;

    private final Log logger = HttpLogging.forLogName(getClass());

    private final Map<Class<?>, Class<?>> mixIns = new LinkedHashMap<>();

    private final Map<Class<?>, JsonSerializer<?>> serializers = new LinkedHashMap<>();

    private final Map<Class<?>, JsonDeserializer<?>> deserializers = new LinkedHashMap<>();

    private final Map<PropertyAccessor, JsonAutoDetect.Visibility> visibilities = new LinkedHashMap<>();

    private final Map<Object, Boolean> features = new LinkedHashMap<>();

    private boolean createXmlMapper = false;

    @Nullable
    private JsonFactory factory;

    @Nullable
    private DateFormat dateFormat;

    @Nullable
    private Locale locale;

    @Nullable
    private TimeZone timeZone;

    @Nullable
    private AnnotationIntrospector annotationIntrospector;

    @Nullable
    private PropertyNamingStrategy propertyNamingStrategy;

    @Nullable
    private TypeResolverBuilder<?> defaultTyping;

    @Nullable
    private JsonInclude.Include serializationInclusion;

    @Nullable
    private FilterProvider filters;

    @Nullable
    private List<Module> modules;

    @Nullable
    private Class<? extends Module>[] moduleClasses;

    private boolean findModulesViaServiceLoader = false;

    private boolean findWellKnownModules = true;

    private ClassLoader moduleClassLoader = getClass().getClassLoader();

    @Nullable
    private HandlerInstantiator handlerInstantiator;

    @Nullable
    private ApplicationContext applicationContext;

    @Nullable
    private Boolean defaultUseWrapper;

    public Jackson2ObjectMapperBuilder createXmlMapper(boolean createXmlMapper) {
        this.createXmlMapper = createXmlMapper;
        return this;
    }

    public Jackson2ObjectMapperBuilder factory(JsonFactory factory) {
        this.factory = factory;
        return this;
    }

    public Jackson2ObjectMapperBuilder dateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public Jackson2ObjectMapperBuilder simpleDateFormat(String format) {
        this.dateFormat = new SimpleDateFormat(format);
        return this;
    }

    public Jackson2ObjectMapperBuilder locale(Locale locale) {
        this.locale = locale;
        return this;
    }

    public Jackson2ObjectMapperBuilder locale(String localeString) {
        this.locale = StringUtils.parseLocale(localeString);
        return this;
    }

    public Jackson2ObjectMapperBuilder timeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public Jackson2ObjectMapperBuilder timeZone(String timeZoneString) {
        this.timeZone = StringUtils.parseTimeZoneString(timeZoneString);
        return this;
    }

    public Jackson2ObjectMapperBuilder annotationIntrospector(AnnotationIntrospector annotationIntrospector) {
        this.annotationIntrospector = annotationIntrospector;
        return this;
    }

    public Jackson2ObjectMapperBuilder propertyNamingStrategy(PropertyNamingStrategy propertyNamingStrategy) {
        this.propertyNamingStrategy = propertyNamingStrategy;
        return this;
    }

    public Jackson2ObjectMapperBuilder defaultTyping(TypeResolverBuilder<?> typeResolverBuilder) {
        this.defaultTyping = typeResolverBuilder;
        return this;
    }

    public Jackson2ObjectMapperBuilder serializationInclusion(JsonInclude.Include serializationInclusion) {
        this.serializationInclusion = serializationInclusion;
        return this;
    }

    public Jackson2ObjectMapperBuilder filters(FilterProvider filters) {
        this.filters = filters;
        return this;
    }

    public Jackson2ObjectMapperBuilder mixIn(Class<?> target, Class<?> mixinSource) {
        this.mixIns.put(target, mixinSource);
        return this;
    }

    public Jackson2ObjectMapperBuilder mixIns(Map<Class<?>, Class<?>> mixIns) {
        this.mixIns.putAll(mixIns);
        return this;
    }

    public Jackson2ObjectMapperBuilder serializers(JsonSerializer<?>... serializers) {
        for (JsonSerializer<?> serializer : serializers) {
            Class<?> handledType = serializer.handledType();
            if (handledType == null || handledType == Object.class) {
                throw new IllegalArgumentException("Unknown handled type in " + serializer.getClass().getName());
            }
            this.serializers.put(serializer.handledType(), serializer);
        }
        return this;
    }

    public Jackson2ObjectMapperBuilder serializerByType(Class<?> type, JsonSerializer<?> serializer) {
        this.serializers.put(type, serializer);
        return this;
    }

    public Jackson2ObjectMapperBuilder serializersByType(Map<Class<?>, JsonSerializer<?>> serializers) {
        this.serializers.putAll(serializers);
        return this;
    }

    public Jackson2ObjectMapperBuilder deserializers(JsonDeserializer<?>... deserializers) {
        for (JsonDeserializer<?> deserializer : deserializers) {
            Class<?> handledType = deserializer.handledType();
            if (handledType == null || handledType == Object.class) {
                throw new IllegalArgumentException("Unknown handled type in " + deserializer.getClass().getName());
            }
            this.deserializers.put(deserializer.handledType(), deserializer);
        }
        return this;
    }

    public Jackson2ObjectMapperBuilder deserializerByType(Class<?> type, JsonDeserializer<?> deserializer) {
        this.deserializers.put(type, deserializer);
        return this;
    }

    public Jackson2ObjectMapperBuilder deserializersByType(Map<Class<?>, JsonDeserializer<?>> deserializers) {
        this.deserializers.putAll(deserializers);
        return this;
    }

    public Jackson2ObjectMapperBuilder autoDetectFields(boolean autoDetectFields) {
        this.features.put(MapperFeature.AUTO_DETECT_FIELDS, autoDetectFields);
        return this;
    }

    public Jackson2ObjectMapperBuilder autoDetectGettersSetters(boolean autoDetectGettersSetters) {
        this.features.put(MapperFeature.AUTO_DETECT_GETTERS, autoDetectGettersSetters);
        this.features.put(MapperFeature.AUTO_DETECT_SETTERS, autoDetectGettersSetters);
        this.features.put(MapperFeature.AUTO_DETECT_IS_GETTERS, autoDetectGettersSetters);
        return this;
    }

    public Jackson2ObjectMapperBuilder defaultViewInclusion(boolean defaultViewInclusion) {
        this.features.put(MapperFeature.DEFAULT_VIEW_INCLUSION, defaultViewInclusion);
        return this;
    }

    public Jackson2ObjectMapperBuilder failOnUnknownProperties(boolean failOnUnknownProperties) {
        this.features.put(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
        return this;
    }

    public Jackson2ObjectMapperBuilder failOnEmptyBeans(boolean failOnEmptyBeans) {
        this.features.put(SerializationFeature.FAIL_ON_EMPTY_BEANS, failOnEmptyBeans);
        return this;
    }

    public Jackson2ObjectMapperBuilder indentOutput(boolean indentOutput) {
        this.features.put(SerializationFeature.INDENT_OUTPUT, indentOutput);
        return this;
    }

    public Jackson2ObjectMapperBuilder defaultUseWrapper(boolean defaultUseWrapper) {
        this.defaultUseWrapper = defaultUseWrapper;
        return this;
    }

    public Jackson2ObjectMapperBuilder visibility(PropertyAccessor accessor, JsonAutoDetect.Visibility visibility) {
        this.visibilities.put(accessor, visibility);
        return this;
    }

    public Jackson2ObjectMapperBuilder featuresToEnable(Object... featuresToEnable) {
        for (Object feature : featuresToEnable) {
            this.features.put(feature, Boolean.TRUE);
        }
        return this;
    }

    public Jackson2ObjectMapperBuilder featuresToDisable(Object... featuresToDisable) {
        for (Object feature : featuresToDisable) {
            this.features.put(feature, Boolean.FALSE);
        }
        return this;
    }

    public Jackson2ObjectMapperBuilder modules(Module... modules) {
        return modules(Arrays.asList(modules));
    }

    public Jackson2ObjectMapperBuilder modules(List<Module> modules) {
        this.modules = new LinkedList<>(modules);
        this.findModulesViaServiceLoader = false;
        this.findWellKnownModules = false;
        return this;
    }

    public Jackson2ObjectMapperBuilder modulesToInstall(Module... modules) {
        this.modules = Arrays.asList(modules);
        this.findWellKnownModules = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    public Jackson2ObjectMapperBuilder modulesToInstall(Class<? extends Module>... modules) {
        this.moduleClasses = modules;
        this.findWellKnownModules = true;
        return this;
    }

    public Jackson2ObjectMapperBuilder findModulesViaServiceLoader(boolean findModules) {
        this.findModulesViaServiceLoader = findModules;
        return this;
    }

    public Jackson2ObjectMapperBuilder moduleClassLoader(ClassLoader moduleClassLoader) {
        this.moduleClassLoader = moduleClassLoader;
        return this;
    }

    public Jackson2ObjectMapperBuilder handlerInstantiator(HandlerInstantiator handlerInstantiator) {
        this.handlerInstantiator = handlerInstantiator;
        return this;
    }

    public Jackson2ObjectMapperBuilder applicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T extends ObjectMapper> T build() {
        ObjectMapper mapper;
        if (this.createXmlMapper) {
            mapper = (this.defaultUseWrapper != null ? new XmlObjectMapperInitializer().create(this.defaultUseWrapper, this.factory) : new XmlObjectMapperInitializer().create(this.factory));
        } else {
            mapper = (this.factory != null ? new ObjectMapper(this.factory) : new ObjectMapper());
        }
        configure(mapper);
        return (T) mapper;
    }

    public void configure(ObjectMapper objectMapper) {
        Assert.notNull(objectMapper, "ObjectMapper must not be null");
        MultiValueMap<Object, Module> modulesToRegister = new LinkedMultiValueMap<>();
        if (this.findModulesViaServiceLoader) {
            ObjectMapper.findModules(this.moduleClassLoader).forEach(module -> registerModule(module, modulesToRegister));
        } else if (this.findWellKnownModules) {
            registerWellKnownModulesIfAvailable(modulesToRegister);
        }
        if (this.modules != null) {
            this.modules.forEach(module -> registerModule(module, modulesToRegister));
        }
        if (this.moduleClasses != null) {
            for (Class<? extends Module> moduleClass : this.moduleClasses) {
                registerModule(BeanUtils.instantiateClass(moduleClass), modulesToRegister);
            }
        }
        List<Module> modules = new ArrayList<>();
        for (List<Module> nestedModules : modulesToRegister.values()) {
            modules.addAll(nestedModules);
        }
        objectMapper.registerModules(modules);
        if (this.dateFormat != null) {
            objectMapper.setDateFormat(this.dateFormat);
        }
        if (this.locale != null) {
            objectMapper.setLocale(this.locale);
        }
        if (this.timeZone != null) {
            objectMapper.setTimeZone(this.timeZone);
        }
        if (this.annotationIntrospector != null) {
            objectMapper.setAnnotationIntrospector(this.annotationIntrospector);
        }
        if (this.propertyNamingStrategy != null) {
            objectMapper.setPropertyNamingStrategy(this.propertyNamingStrategy);
        }
        if (this.defaultTyping != null) {
            objectMapper.setDefaultTyping(this.defaultTyping);
        }
        if (this.serializationInclusion != null) {
            objectMapper.setSerializationInclusion(this.serializationInclusion);
        }
        if (this.filters != null) {
            objectMapper.setFilterProvider(this.filters);
        }
        this.mixIns.forEach(objectMapper::addMixIn);
        if (!this.serializers.isEmpty() || !this.deserializers.isEmpty()) {
            SimpleModule module = new SimpleModule();
            addSerializers(module);
            addDeserializers(module);
            objectMapper.registerModule(module);
        }
        this.visibilities.forEach(objectMapper::setVisibility);
        customizeDefaultFeatures(objectMapper);
        this.features.forEach((feature, enabled) -> configureFeature(objectMapper, feature, enabled));
        if (this.handlerInstantiator != null) {
            objectMapper.setHandlerInstantiator(this.handlerInstantiator);
        } else if (this.applicationContext != null) {
            objectMapper.setHandlerInstantiator(new SpringHandlerInstantiator(this.applicationContext.getAutowireCapableBeanFactory()));
        }
    }

    private void registerModule(Module module, MultiValueMap<Object, Module> modulesToRegister) {
        if (module.getTypeId() == null) {
            modulesToRegister.add(SimpleModule.class.getName(), module);
        } else {
            modulesToRegister.set(module.getTypeId(), module);
        }
    }

    // Any change to this method should be also applied to spring-jms and spring-messaging
    // MappingJackson2MessageConverter default constructors
    private void customizeDefaultFeatures(ObjectMapper objectMapper) {
        if (!this.features.containsKey(MapperFeature.DEFAULT_VIEW_INCLUSION)) {
            configureFeature(objectMapper, MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        }
        if (!this.features.containsKey(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)) {
            configureFeature(objectMapper, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void addSerializers(SimpleModule module) {
        this.serializers.forEach((type, serializer) -> module.addSerializer((Class<? extends T>) type, (JsonSerializer<T>) serializer));
    }

    @SuppressWarnings("unchecked")
    private <T> void addDeserializers(SimpleModule module) {
        this.deserializers.forEach((type, deserializer) -> module.addDeserializer((Class<T>) type, (JsonDeserializer<? extends T>) deserializer));
    }

    private void configureFeature(ObjectMapper objectMapper, Object feature, boolean enabled) {
        if (feature instanceof JsonParser.Feature) {
            objectMapper.configure((JsonParser.Feature) feature, enabled);
        } else if (feature instanceof JsonGenerator.Feature) {
            objectMapper.configure((JsonGenerator.Feature) feature, enabled);
        } else if (feature instanceof SerializationFeature) {
            objectMapper.configure((SerializationFeature) feature, enabled);
        } else if (feature instanceof DeserializationFeature) {
            objectMapper.configure((DeserializationFeature) feature, enabled);
        } else if (feature instanceof MapperFeature) {
            objectMapper.configure((MapperFeature) feature, enabled);
        } else {
            throw new FatalBeanException("Unknown feature class: " + feature.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private void registerWellKnownModulesIfAvailable(MultiValueMap<Object, Module> modulesToRegister) {
        try {
            Class<? extends Module> jdk8ModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.datatype.jdk8.Jdk8Module", this.moduleClassLoader);
            Module jdk8Module = BeanUtils.instantiateClass(jdk8ModuleClass);
            modulesToRegister.set(jdk8Module.getTypeId(), jdk8Module);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jdk8 not available
        }
        try {
            Class<? extends Module> javaTimeModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", this.moduleClassLoader);
            Module javaTimeModule = BeanUtils.instantiateClass(javaTimeModuleClass);
            modulesToRegister.set(javaTimeModule.getTypeId(), javaTimeModule);
        } catch (ClassNotFoundException ex) {
            // jackson-datatype-jsr310 not available
        }
        // Joda-Time present?
        if (ClassUtils.isPresent("org.joda.time.LocalDate", this.moduleClassLoader)) {
            try {
                Class<? extends Module> jodaModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.datatype.joda.JodaModule", this.moduleClassLoader);
                Module jodaModule = BeanUtils.instantiateClass(jodaModuleClass);
                modulesToRegister.set(jodaModule.getTypeId(), jodaModule);
            } catch (ClassNotFoundException ex) {
                // jackson-datatype-joda not available
            }
        }
        // Kotlin present?
        if (KotlinDetector.isKotlinPresent()) {
            try {
                Class<? extends Module> kotlinModuleClass = (Class<? extends Module>) ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", this.moduleClassLoader);
                Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
                modulesToRegister.set(kotlinModule.getTypeId(), kotlinModule);
            } catch (ClassNotFoundException ex) {
                if (!kotlinWarningLogged) {
                    kotlinWarningLogged = true;
                    logger.warn("For Jackson Kotlin classes support please add " + "\"com.fasterxml.jackson.module:jackson-module-kotlin\" to the classpath");
                }
            }
        }
    }
    // Convenience factory methods

    public static Jackson2ObjectMapperBuilder json() {
        return new Jackson2ObjectMapperBuilder();
    }

    public static Jackson2ObjectMapperBuilder xml() {
        return new Jackson2ObjectMapperBuilder().createXmlMapper(true);
    }

    public static Jackson2ObjectMapperBuilder smile() {
        return new Jackson2ObjectMapperBuilder().factory(new SmileFactoryInitializer().create());
    }

    public static Jackson2ObjectMapperBuilder cbor() {
        return new Jackson2ObjectMapperBuilder().factory(new CborFactoryInitializer().create());
    }

    private static class XmlObjectMapperInitializer {

        public ObjectMapper create(@Nullable JsonFactory factory) {
            if (factory != null) {
                return new XmlMapper((XmlFactory) factory);
            } else {
                return new XmlMapper(StaxUtils.createDefensiveInputFactory());
            }
        }

        public ObjectMapper create(boolean defaultUseWrapper, @Nullable JsonFactory factory) {
            JacksonXmlModule module = new JacksonXmlModule();
            module.setDefaultUseWrapper(defaultUseWrapper);
            if (factory != null) {
                return new XmlMapper((XmlFactory) factory, module);
            } else {
                return new XmlMapper(new XmlFactory(StaxUtils.createDefensiveInputFactory()), module);
            }
        }

    }

    private static class SmileFactoryInitializer {

        public JsonFactory create() {
            return new SmileFactory();
        }

    }

    private static class CborFactoryInitializer {

        public JsonFactory create() {
            return new CBORFactory();
        }

    }

}
