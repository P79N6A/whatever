package org.springframework.beans.support;

import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyEditor;
import java.lang.reflect.Method;

public class ArgumentConvertingMethodInvoker extends MethodInvoker {

    @Nullable
    private TypeConverter typeConverter;

    private boolean useDefaultConverter = true;

    public void setTypeConverter(@Nullable TypeConverter typeConverter) {
        this.typeConverter = typeConverter;
        this.useDefaultConverter = (typeConverter == null);
    }

    @Nullable
    public TypeConverter getTypeConverter() {
        if (this.typeConverter == null && this.useDefaultConverter) {
            this.typeConverter = getDefaultTypeConverter();
        }
        return this.typeConverter;
    }

    protected TypeConverter getDefaultTypeConverter() {
        return new SimpleTypeConverter();
    }

    public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        TypeConverter converter = getTypeConverter();
        if (!(converter instanceof PropertyEditorRegistry)) {
            throw new IllegalStateException("TypeConverter does not implement PropertyEditorRegistry interface: " + converter);
        }
        ((PropertyEditorRegistry) converter).registerCustomEditor(requiredType, propertyEditor);
    }

    @Override
    protected Method findMatchingMethod() {
        Method matchingMethod = super.findMatchingMethod();
        // Second pass: look for method where arguments can be converted to parameter types.
        if (matchingMethod == null) {
            // Interpret argument array as individual method arguments.
            matchingMethod = doFindMatchingMethod(getArguments());
        }
        if (matchingMethod == null) {
            // Interpret argument array as single method argument of array type.
            matchingMethod = doFindMatchingMethod(new Object[]{getArguments()});
        }
        return matchingMethod;
    }

    @Nullable
    protected Method doFindMatchingMethod(Object[] arguments) {
        TypeConverter converter = getTypeConverter();
        if (converter != null) {
            String targetMethod = getTargetMethod();
            Method matchingMethod = null;
            int argCount = arguments.length;
            Class<?> targetClass = getTargetClass();
            Assert.state(targetClass != null, "No target class set");
            Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
            int minTypeDiffWeight = Integer.MAX_VALUE;
            Object[] argumentsToUse = null;
            for (Method candidate : candidates) {
                if (candidate.getName().equals(targetMethod)) {
                    // Check if the inspected method has the correct number of parameters.
                    Class<?>[] paramTypes = candidate.getParameterTypes();
                    if (paramTypes.length == argCount) {
                        Object[] convertedArguments = new Object[argCount];
                        boolean match = true;
                        for (int j = 0; j < argCount && match; j++) {
                            // Verify that the supplied argument is assignable to the method parameter.
                            try {
                                convertedArguments[j] = converter.convertIfNecessary(arguments[j], paramTypes[j]);
                            } catch (TypeMismatchException ex) {
                                // Ignore -> simply doesn't match.
                                match = false;
                            }
                        }
                        if (match) {
                            int typeDiffWeight = getTypeDifferenceWeight(paramTypes, convertedArguments);
                            if (typeDiffWeight < minTypeDiffWeight) {
                                minTypeDiffWeight = typeDiffWeight;
                                matchingMethod = candidate;
                                argumentsToUse = convertedArguments;
                            }
                        }
                    }
                }
            }
            if (matchingMethod != null) {
                setArguments(argumentsToUse);
                return matchingMethod;
            }
        }
        return null;
    }

}
