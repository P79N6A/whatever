package org.springframework.validation.beanvalidation;

import org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

import javax.validation.*;
import javax.validation.bootstrap.GenericBootstrap;
import javax.validation.bootstrap.ProviderSpecificBootstrap;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class LocalValidatorFactoryBean extends SpringValidatorAdapter implements ValidatorFactory, ApplicationContextAware, InitializingBean, DisposableBean {

    public ClockProvider getClockProvider() {
        //
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Nullable
    private Class providerClass;

    @Nullable
    private ValidationProviderResolver validationProviderResolver;

    @Nullable
    private MessageInterpolator messageInterpolator;

    @Nullable
    private TraversableResolver traversableResolver;

    @Nullable
    private ConstraintValidatorFactory constraintValidatorFactory;

    @Nullable
    private ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Nullable
    private Resource[] mappingLocations;

    private final Map<String, String> validationPropertyMap = new HashMap<>();

    @Nullable
    private ApplicationContext applicationContext;

    @Nullable
    private ValidatorFactory validatorFactory;

    @SuppressWarnings("rawtypes")
    public void setProviderClass(Class providerClass) {
        this.providerClass = providerClass;
    }

    public void setValidationProviderResolver(ValidationProviderResolver validationProviderResolver) {
        this.validationProviderResolver = validationProviderResolver;
    }

    public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
        this.messageInterpolator = messageInterpolator;
    }

    public void setValidationMessageSource(MessageSource messageSource) {
        this.messageInterpolator = HibernateValidatorDelegate.buildMessageInterpolator(messageSource);
    }

    public void setTraversableResolver(TraversableResolver traversableResolver) {
        this.traversableResolver = traversableResolver;
    }

    public void setConstraintValidatorFactory(ConstraintValidatorFactory constraintValidatorFactory) {
        this.constraintValidatorFactory = constraintValidatorFactory;
    }

    public void setParameterNameDiscoverer(ParameterNameDiscoverer parameterNameDiscoverer) {
        this.parameterNameDiscoverer = parameterNameDiscoverer;
    }

    public void setMappingLocations(Resource... mappingLocations) {
        this.mappingLocations = mappingLocations;
    }

    public void setValidationProperties(Properties jpaProperties) {
        CollectionUtils.mergePropertiesIntoMap(jpaProperties, this.validationPropertyMap);
    }

    public void setValidationPropertyMap(@Nullable Map<String, String> validationProperties) {
        if (validationProperties != null) {
            this.validationPropertyMap.putAll(validationProperties);
        }
    }

    public Map<String, String> getValidationPropertyMap() {
        return this.validationPropertyMap;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void afterPropertiesSet() {
        Configuration<?> configuration;
        if (this.providerClass != null) {
            ProviderSpecificBootstrap bootstrap = Validation.byProvider(this.providerClass);
            if (this.validationProviderResolver != null) {
                bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
            }
            configuration = bootstrap.configure();
        } else {
            GenericBootstrap bootstrap = Validation.byDefaultProvider();
            if (this.validationProviderResolver != null) {
                bootstrap = bootstrap.providerResolver(this.validationProviderResolver);
            }
            configuration = bootstrap.configure();
        }
        // Try Hibernate Validator 5.2's externalClassLoader(ClassLoader) method
        if (this.applicationContext != null) {
            try {
                Method eclMethod = configuration.getClass().getMethod("externalClassLoader", ClassLoader.class);
                ReflectionUtils.invokeMethod(eclMethod, configuration, this.applicationContext.getClassLoader());
            } catch (NoSuchMethodException ex) {
                // Ignore - no Hibernate Validator 5.2+ or similar provider
            }
        }
        MessageInterpolator targetInterpolator = this.messageInterpolator;
        if (targetInterpolator == null) {
            targetInterpolator = configuration.getDefaultMessageInterpolator();
        }
        configuration.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));
        if (this.traversableResolver != null) {
            configuration.traversableResolver(this.traversableResolver);
        }
        ConstraintValidatorFactory targetConstraintValidatorFactory = this.constraintValidatorFactory;
        if (targetConstraintValidatorFactory == null && this.applicationContext != null) {
            targetConstraintValidatorFactory = new SpringConstraintValidatorFactory(this.applicationContext.getAutowireCapableBeanFactory());
        }
        if (targetConstraintValidatorFactory != null) {
            configuration.constraintValidatorFactory(targetConstraintValidatorFactory);
        }
        if (this.parameterNameDiscoverer != null) {
            configureParameterNameProvider(this.parameterNameDiscoverer, configuration);
        }
        if (this.mappingLocations != null) {
            for (Resource location : this.mappingLocations) {
                try {
                    configuration.addMapping(location.getInputStream());
                } catch (IOException ex) {
                    throw new IllegalStateException("Cannot read mapping resource: " + location);
                }
            }
        }
        this.validationPropertyMap.forEach(configuration::addProperty);
        // Allow for custom post-processing before we actually build the ValidatorFactory.
        postProcessConfiguration(configuration);
        this.validatorFactory = configuration.buildValidatorFactory();
        setTargetValidator(this.validatorFactory.getValidator());
    }

    private void configureParameterNameProvider(ParameterNameDiscoverer discoverer, Configuration<?> configuration) {
        final ParameterNameProvider defaultProvider = configuration.getDefaultParameterNameProvider();
        configuration.parameterNameProvider(new ParameterNameProvider() {
            @Override
            public List<String> getParameterNames(Constructor<?> constructor) {
                String[] paramNames = discoverer.getParameterNames(constructor);
                return (paramNames != null ? Arrays.asList(paramNames) : defaultProvider.getParameterNames(constructor));
            }

            @Override
            public List<String> getParameterNames(Method method) {
                String[] paramNames = discoverer.getParameterNames(method);
                return (paramNames != null ? Arrays.asList(paramNames) : defaultProvider.getParameterNames(method));
            }
        });
    }

    protected void postProcessConfiguration(Configuration<?> configuration) {
    }

    @Override
    public Validator getValidator() {
        Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
        return this.validatorFactory.getValidator();
    }

    @Override
    public ValidatorContext usingContext() {
        Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
        return this.validatorFactory.usingContext();
    }

    @Override
    public MessageInterpolator getMessageInterpolator() {
        Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
        return this.validatorFactory.getMessageInterpolator();
    }

    @Override
    public TraversableResolver getTraversableResolver() {
        Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
        return this.validatorFactory.getTraversableResolver();
    }

    @Override
    public ConstraintValidatorFactory getConstraintValidatorFactory() {
        Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
        return this.validatorFactory.getConstraintValidatorFactory();
    }

    @Override
    public ParameterNameProvider getParameterNameProvider() {
        Assert.notNull(this.validatorFactory, "No target ValidatorFactory set");
        return this.validatorFactory.getParameterNameProvider();
    }
    // Bean Validation 2.0: currently not implemented here since it would imply
    // a hard dependency on the new javax.validation.ClockProvider interface.
    // To be resolved once Spring Framework requires Bean Validation 2.0+.
    // Obtain the native ValidatorFactory through unwrap(ValidatorFactory.class)
    // instead which will fully support a getClockProvider() call as well.

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(@Nullable Class<T> type) {
        if (type == null || !ValidatorFactory.class.isAssignableFrom(type)) {
            try {
                return super.unwrap(type);
            } catch (ValidationException ex) {
                // ignore - we'll try ValidatorFactory unwrapping next
            }
        }
        if (this.validatorFactory != null) {
            try {
                return this.validatorFactory.unwrap(type);
            } catch (ValidationException ex) {
                // ignore if just being asked for ValidatorFactory
                if (ValidatorFactory.class == type) {
                    return (T) this.validatorFactory;
                }
                throw ex;
            }
        }
        throw new ValidationException("Cannot unwrap to " + type);
    }

    public void close() {
        if (this.validatorFactory != null) {
            this.validatorFactory.close();
        }
    }

    @Override
    public void destroy() {
        close();
    }

    private static class HibernateValidatorDelegate {

        public static MessageInterpolator buildMessageInterpolator(MessageSource messageSource) {
            return new ResourceBundleMessageInterpolator(new MessageSourceResourceBundleLocator(messageSource));
        }

    }

}
