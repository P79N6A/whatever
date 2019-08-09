package org.springframework.expression.spel.support;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;

public abstract class ReflectionHelper {

    @Nullable
    static ArgumentsMatchInfo compareArguments(List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {
        Assert.isTrue(expectedArgTypes.size() == suppliedArgTypes.size(), "Expected argument types and supplied argument types should be arrays of same length");
        ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;
        for (int i = 0; i < expectedArgTypes.size() && match != null; i++) {
            TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
            TypeDescriptor expectedArg = expectedArgTypes.get(i);
            // The user may supply null - and that will be ok unless a primitive is expected
            if (suppliedArg == null) {
                if (expectedArg.isPrimitive()) {
                    match = null;
                }
            } else if (!expectedArg.equals(suppliedArg)) {
                if (suppliedArg.isAssignableTo(expectedArg)) {
                    if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
                        match = ArgumentsMatchKind.CLOSE;
                    }
                } else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
                    match = ArgumentsMatchKind.REQUIRES_CONVERSION;
                } else {
                    match = null;
                }
            }
        }
        return (match != null ? new ArgumentsMatchInfo(match) : null);
    }

    public static int getTypeDifferenceWeight(List<TypeDescriptor> paramTypes, List<TypeDescriptor> argTypes) {
        int result = 0;
        for (int i = 0; i < paramTypes.size(); i++) {
            TypeDescriptor paramType = paramTypes.get(i);
            TypeDescriptor argType = (i < argTypes.size() ? argTypes.get(i) : null);
            if (argType == null) {
                if (paramType.isPrimitive()) {
                    return Integer.MAX_VALUE;
                }
            } else {
                Class<?> paramTypeClazz = paramType.getType();
                if (!ClassUtils.isAssignable(paramTypeClazz, argType.getType())) {
                    return Integer.MAX_VALUE;
                }
                if (paramTypeClazz.isPrimitive()) {
                    paramTypeClazz = Object.class;
                }
                Class<?> superClass = argType.getType().getSuperclass();
                while (superClass != null) {
                    if (paramTypeClazz.equals(superClass)) {
                        result = result + 2;
                        superClass = null;
                    } else if (ClassUtils.isAssignable(paramTypeClazz, superClass)) {
                        result = result + 2;
                        superClass = superClass.getSuperclass();
                    } else {
                        superClass = null;
                    }
                }
                if (paramTypeClazz.isInterface()) {
                    result = result + 1;
                }
            }
        }
        return result;
    }

    @Nullable
    static ArgumentsMatchInfo compareArgumentsVarargs(List<TypeDescriptor> expectedArgTypes, List<TypeDescriptor> suppliedArgTypes, TypeConverter typeConverter) {
        Assert.isTrue(!CollectionUtils.isEmpty(expectedArgTypes), "Expected arguments must at least include one array (the varargs parameter)");
        Assert.isTrue(expectedArgTypes.get(expectedArgTypes.size() - 1).isArray(), "Final expected argument should be array type (the varargs parameter)");
        ArgumentsMatchKind match = ArgumentsMatchKind.EXACT;
        // Check up until the varargs argument:
        // Deal with the arguments up to 'expected number' - 1 (that is everything but the varargs argument)
        int argCountUpToVarargs = expectedArgTypes.size() - 1;
        for (int i = 0; i < argCountUpToVarargs && match != null; i++) {
            TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
            TypeDescriptor expectedArg = expectedArgTypes.get(i);
            if (suppliedArg == null) {
                if (expectedArg.isPrimitive()) {
                    match = null;
                }
            } else {
                if (!expectedArg.equals(suppliedArg)) {
                    if (suppliedArg.isAssignableTo(expectedArg)) {
                        if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
                            match = ArgumentsMatchKind.CLOSE;
                        }
                    } else if (typeConverter.canConvert(suppliedArg, expectedArg)) {
                        match = ArgumentsMatchKind.REQUIRES_CONVERSION;
                    } else {
                        match = null;
                    }
                }
            }
        }
        // If already confirmed it cannot be a match, then return
        if (match == null) {
            return null;
        }
        if (suppliedArgTypes.size() == expectedArgTypes.size() && expectedArgTypes.get(expectedArgTypes.size() - 1).equals(suppliedArgTypes.get(suppliedArgTypes.size() - 1))) {
            // Special case: there is one parameter left and it is an array and it matches the varargs
            // expected argument - that is a match, the caller has already built the array. Proceed with it.
        } else {
            // Now... we have the final argument in the method we are checking as a match and we have 0
            // or more other arguments left to pass to it.
            TypeDescriptor varargsDesc = expectedArgTypes.get(expectedArgTypes.size() - 1);
            TypeDescriptor elementDesc = varargsDesc.getElementTypeDescriptor();
            Assert.state(elementDesc != null, "No element type");
            Class<?> varargsParamType = elementDesc.getType();
            // All remaining parameters must be of this type or convertible to this type
            for (int i = expectedArgTypes.size() - 1; i < suppliedArgTypes.size(); i++) {
                TypeDescriptor suppliedArg = suppliedArgTypes.get(i);
                if (suppliedArg == null) {
                    if (varargsParamType.isPrimitive()) {
                        match = null;
                    }
                } else {
                    if (varargsParamType != suppliedArg.getType()) {
                        if (ClassUtils.isAssignable(varargsParamType, suppliedArg.getType())) {
                            if (match != ArgumentsMatchKind.REQUIRES_CONVERSION) {
                                match = ArgumentsMatchKind.CLOSE;
                            }
                        } else if (typeConverter.canConvert(suppliedArg, TypeDescriptor.valueOf(varargsParamType))) {
                            match = ArgumentsMatchKind.REQUIRES_CONVERSION;
                        } else {
                            match = null;
                        }
                    }
                }
            }
        }
        return (match != null ? new ArgumentsMatchInfo(match) : null);
    }
    // TODO could do with more refactoring around argument handling and varargs

    public static boolean convertAllArguments(TypeConverter converter, Object[] arguments, Method method) throws SpelEvaluationException {
        Integer varargsPosition = (method.isVarArgs() ? method.getParameterCount() - 1 : null);
        return convertArguments(converter, arguments, method, varargsPosition);
    }

    static boolean convertArguments(TypeConverter converter, Object[] arguments, Executable executable, @Nullable Integer varargsPosition) throws EvaluationException {
        boolean conversionOccurred = false;
        if (varargsPosition == null) {
            for (int i = 0; i < arguments.length; i++) {
                TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forExecutable(executable, i));
                Object argument = arguments[i];
                arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
                conversionOccurred |= (argument != arguments[i]);
            }
        } else {
            // Convert everything up to the varargs position
            for (int i = 0; i < varargsPosition; i++) {
                TypeDescriptor targetType = new TypeDescriptor(MethodParameter.forExecutable(executable, i));
                Object argument = arguments[i];
                arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
                conversionOccurred |= (argument != arguments[i]);
            }
            MethodParameter methodParam = MethodParameter.forExecutable(executable, varargsPosition);
            if (varargsPosition == arguments.length - 1) {
                // If the target is varargs and there is just one more argument
                // then convert it here
                TypeDescriptor targetType = new TypeDescriptor(methodParam);
                Object argument = arguments[varargsPosition];
                TypeDescriptor sourceType = TypeDescriptor.forObject(argument);
                arguments[varargsPosition] = converter.convertValue(argument, sourceType, targetType);
                // Three outcomes of that previous line:
                // 1) the input argument was already compatible (ie. array of valid type) and nothing was done
                // 2) the input argument was correct type but not in an array so it was made into an array
                // 3) the input argument was the wrong type and got converted and put into an array
                if (argument != arguments[varargsPosition] && !isFirstEntryInArray(argument, arguments[varargsPosition])) {
                    conversionOccurred = true; // case 3
                }
            } else {
                // Convert remaining arguments to the varargs element type
                TypeDescriptor targetType = new TypeDescriptor(methodParam).getElementTypeDescriptor();
                Assert.state(targetType != null, "No element type");
                for (int i = varargsPosition; i < arguments.length; i++) {
                    Object argument = arguments[i];
                    arguments[i] = converter.convertValue(argument, TypeDescriptor.forObject(argument), targetType);
                    conversionOccurred |= (argument != arguments[i]);
                }
            }
        }
        return conversionOccurred;
    }

    private static boolean isFirstEntryInArray(Object value, @Nullable Object possibleArray) {
        if (possibleArray == null) {
            return false;
        }
        Class<?> type = possibleArray.getClass();
        if (!type.isArray() || Array.getLength(possibleArray) == 0 || !ClassUtils.isAssignableValue(type.getComponentType(), value)) {
            return false;
        }
        Object arrayValue = Array.get(possibleArray, 0);
        return (type.getComponentType().isPrimitive() ? arrayValue.equals(value) : arrayValue == value);
    }

    public static Object[] setupArgumentsForVarargsInvocation(Class<?>[] requiredParameterTypes, Object... args) {
        // Check if array already built for final argument
        int parameterCount = requiredParameterTypes.length;
        int argumentCount = args.length;
        // Check if repackaging is needed...
        if (parameterCount != args.length || requiredParameterTypes[parameterCount - 1] != (args[argumentCount - 1] != null ? args[argumentCount - 1].getClass() : null)) {
            int arraySize = 0;  // zero size array if nothing to pass as the varargs parameter
            if (argumentCount >= parameterCount) {
                arraySize = argumentCount - (parameterCount - 1);
            }
            // Create an array for the varargs arguments
            Object[] newArgs = new Object[parameterCount];
            System.arraycopy(args, 0, newArgs, 0, newArgs.length - 1);
            // Now sort out the final argument, which is the varargs one. Before entering this method,
            // the arguments should have been converted to the box form of the required type.
            Class<?> componentType = requiredParameterTypes[parameterCount - 1].getComponentType();
            Object repackagedArgs = Array.newInstance(componentType, arraySize);
            for (int i = 0; i < arraySize; i++) {
                Array.set(repackagedArgs, i, args[parameterCount - 1 + i]);
            }
            newArgs[newArgs.length - 1] = repackagedArgs;
            return newArgs;
        }
        return args;
    }

    enum ArgumentsMatchKind {

        EXACT,

        CLOSE,

        REQUIRES_CONVERSION
    }

    static class ArgumentsMatchInfo {

        private final ArgumentsMatchKind kind;

        ArgumentsMatchInfo(ArgumentsMatchKind kind) {
            this.kind = kind;
        }

        public boolean isExactMatch() {
            return (this.kind == ArgumentsMatchKind.EXACT);
        }

        public boolean isCloseMatch() {
            return (this.kind == ArgumentsMatchKind.CLOSE);
        }

        public boolean isMatchRequiringConversion() {
            return (this.kind == ArgumentsMatchKind.REQUIRES_CONVERSION);
        }

        @Override
        public String toString() {
            return "ArgumentMatchInfo: " + this.kind;
        }

    }

}
