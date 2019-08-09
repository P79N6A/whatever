package org.springframework.boot.context.properties.bind;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.KotlinDetector;
import org.springframework.core.ResolvableType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

class ConstructorParametersBinder implements BeanBinder {

    private static boolean KOTLIN_PRESENT = KotlinDetector.isKotlinPresent();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T bind(ConfigurationPropertyName name, Bindable<T> target, Binder.Context context, BeanPropertyBinder propertyBinder) {
        Bean bean = Bean.get(target);
        if (bean == null) {
            return null;
        }
        List<Object> bound = bind(propertyBinder, bean, context.getConverter());
        return (T) BeanUtils.instantiateClass(bean.getConstructor(), bound.toArray());
    }

    private List<Object> bind(BeanPropertyBinder propertyBinder, Bean bean, BindConverter converter) {
        Collection<ConstructorParameter> parameters = bean.getParameters().values();
        List<Object> boundParameters = new ArrayList<>(parameters.size());
        for (ConstructorParameter parameter : parameters) {
            Object boundParameter = bind(parameter, propertyBinder);
            if (boundParameter == null) {
                boundParameter = getDefaultValue(parameter, converter);
            }
            boundParameters.add(boundParameter);
        }
        return boundParameters;
    }

    private Object getDefaultValue(ConstructorParameter parameter, BindConverter converter) {
        if (parameter.getDefaultValue() == null) {
            return null;
        }
        return converter.convert(parameter.getDefaultValue(), parameter.getType(), parameter.getAnnotations());
    }

    private Object bind(ConstructorParameter parameter, BeanPropertyBinder propertyBinder) {
        String propertyName = parameter.getName();
        ResolvableType type = parameter.getType();
        return propertyBinder.bindProperty(propertyName, Bindable.of(type));
    }

    private static final class Bean {

        private final Constructor<?> constructor;

        private final Map<String, ConstructorParameter> parameters;

        private Bean(Constructor<?> constructor, Map<String, ConstructorParameter> parameters) {
            this.constructor = constructor;
            this.parameters = parameters;
        }

        public static Bean get(Bindable<?> bindable) {
            if (bindable.getValue() != null) {
                return null;
            }
            Class<?> type = bindable.getType().resolve(Object.class);
            if (type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
                return null;
            }
            if (KOTLIN_PRESENT && KotlinDetector.isKotlinType(type)) {
                return KotlinBeanProvider.get(type);
            }
            return SimpleBeanProvider.get(type);
        }

        public Map<String, ConstructorParameter> getParameters() {
            return this.parameters;
        }

        public Constructor<?> getConstructor() {
            return this.constructor;
        }

    }

    private static class KotlinBeanProvider {

        public static Bean get(Class<?> type) {
            Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
            if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
                return get(primaryConstructor);
            }
            return null;
        }

        private static Bean get(Constructor<?> constructor) {
            KFunction<?> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(constructor);
            if (kotlinConstructor != null) {
                return new Bean(constructor, parseParameters(kotlinConstructor));
            }
            return SimpleBeanProvider.get(constructor);
        }

        private static Map<String, ConstructorParameter> parseParameters(KFunction<?> constructor) {
            Map<String, ConstructorParameter> parameters = new LinkedHashMap<>();
            for (KParameter parameter : constructor.getParameters()) {
                String name = parameter.getName();
                Type type = ReflectJvmMapping.getJavaType(parameter.getType());
                Annotation[] annotations = parameter.getAnnotations().toArray(new Annotation[0]);
                parameters.computeIfAbsent(name, (s) -> new ConstructorParameter(name, ResolvableType.forType(type), annotations, null));
            }
            return parameters;
        }

    }

    private static class SimpleBeanProvider {

        public static Bean get(Class<?> type) {
            Constructor<?>[] constructors = type.getDeclaredConstructors();
            if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
                return SimpleBeanProvider.get(constructors[0]);
            }
            return null;
        }

        public static Bean get(Constructor<?> constructor) {
            return new Bean(constructor, parseParameters(constructor));
        }

        private static Map<String, ConstructorParameter> parseParameters(Constructor<?> constructor) {
            Map<String, ConstructorParameter> parameters = new LinkedHashMap<>();
            for (Parameter parameter : constructor.getParameters()) {
                String name = parameter.getName();
                DefaultValue[] annotationsByType = parameter.getAnnotationsByType(DefaultValue.class);
                String[] defaultValue = (annotationsByType.length > 0) ? annotationsByType[0].value() : null;
                parameters.computeIfAbsent(name, (key) -> new ConstructorParameter(name, ResolvableType.forClass(parameter.getType()), parameter.getDeclaredAnnotations(), defaultValue));
            }
            return parameters;
        }

    }

    private static class ConstructorParameter {

        private final String name;

        private final ResolvableType type;

        private final Annotation[] annotations;

        private final String[] defaultValue;

        ConstructorParameter(String name, ResolvableType type, Annotation[] annotations, String[] defaultValue) {
            this.name = BeanPropertyName.toDashedForm(name);
            this.type = type;
            this.annotations = annotations;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return this.name;
        }

        public ResolvableType getType() {
            return this.type;
        }

        public Annotation[] getAnnotations() {
            return this.annotations;
        }

        public String[] getDefaultValue() {
            return this.defaultValue;
        }

    }

}
