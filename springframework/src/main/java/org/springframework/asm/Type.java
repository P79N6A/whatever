package org.springframework.asm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class Type {

    public static final int VOID = 0;

    public static final int BOOLEAN = 1;

    public static final int CHAR = 2;

    public static final int BYTE = 3;

    public static final int SHORT = 4;

    public static final int INT = 5;

    public static final int FLOAT = 6;

    public static final int LONG = 7;

    public static final int DOUBLE = 8;

    public static final int ARRAY = 9;

    public static final int OBJECT = 10;

    public static final int METHOD = 11;

    private static final int INTERNAL = 12;

    private static final String PRIMITIVE_DESCRIPTORS = "VZCBSIFJD";

    public static final Type VOID_TYPE = new Type(VOID, PRIMITIVE_DESCRIPTORS, VOID, VOID + 1);

    public static final Type BOOLEAN_TYPE = new Type(BOOLEAN, PRIMITIVE_DESCRIPTORS, BOOLEAN, BOOLEAN + 1);

    public static final Type CHAR_TYPE = new Type(CHAR, PRIMITIVE_DESCRIPTORS, CHAR, CHAR + 1);

    public static final Type BYTE_TYPE = new Type(BYTE, PRIMITIVE_DESCRIPTORS, BYTE, BYTE + 1);

    public static final Type SHORT_TYPE = new Type(SHORT, PRIMITIVE_DESCRIPTORS, SHORT, SHORT + 1);

    public static final Type INT_TYPE = new Type(INT, PRIMITIVE_DESCRIPTORS, INT, INT + 1);

    public static final Type FLOAT_TYPE = new Type(FLOAT, PRIMITIVE_DESCRIPTORS, FLOAT, FLOAT + 1);

    public static final Type LONG_TYPE = new Type(LONG, PRIMITIVE_DESCRIPTORS, LONG, LONG + 1);

    public static final Type DOUBLE_TYPE = new Type(DOUBLE, PRIMITIVE_DESCRIPTORS, DOUBLE, DOUBLE + 1);
    // -----------------------------------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------------------------------

    private final int sort;

    private final String valueBuffer;

    private final int valueBegin;

    private final int valueEnd;

    private Type(final int sort, final String valueBuffer, final int valueBegin, final int valueEnd) {
        this.sort = sort;
        this.valueBuffer = valueBuffer;
        this.valueBegin = valueBegin;
        this.valueEnd = valueEnd;
    }
    // -----------------------------------------------------------------------------------------------
    // Methods to get Type(s) from a descriptor, a reflected Method or Constructor, other types, etc.
    // -----------------------------------------------------------------------------------------------

    public static Type getType(final String typeDescriptor) {
        return getTypeInternal(typeDescriptor, 0, typeDescriptor.length());
    }

    public static Type getType(final Class<?> clazz) {
        if (clazz.isPrimitive()) {
            if (clazz == Integer.TYPE) {
                return INT_TYPE;
            } else if (clazz == Void.TYPE) {
                return VOID_TYPE;
            } else if (clazz == Boolean.TYPE) {
                return BOOLEAN_TYPE;
            } else if (clazz == Byte.TYPE) {
                return BYTE_TYPE;
            } else if (clazz == Character.TYPE) {
                return CHAR_TYPE;
            } else if (clazz == Short.TYPE) {
                return SHORT_TYPE;
            } else if (clazz == Double.TYPE) {
                return DOUBLE_TYPE;
            } else if (clazz == Float.TYPE) {
                return FLOAT_TYPE;
            } else if (clazz == Long.TYPE) {
                return LONG_TYPE;
            } else {
                throw new AssertionError();
            }
        } else {
            return getType(getDescriptor(clazz));
        }
    }

    public static Type getType(final Constructor<?> constructor) {
        return getType(getConstructorDescriptor(constructor));
    }

    public static Type getType(final Method method) {
        return getType(getMethodDescriptor(method));
    }

    public Type getElementType() {
        final int numDimensions = getDimensions();
        return getTypeInternal(valueBuffer, valueBegin + numDimensions, valueEnd);
    }

    public static Type getObjectType(final String internalName) {
        return new Type(internalName.charAt(0) == '[' ? ARRAY : INTERNAL, internalName, 0, internalName.length());
    }

    public static Type getMethodType(final String methodDescriptor) {
        return new Type(METHOD, methodDescriptor, 0, methodDescriptor.length());
    }

    public static Type getMethodType(final Type returnType, final Type... argumentTypes) {
        return getType(getMethodDescriptor(returnType, argumentTypes));
    }

    public Type[] getArgumentTypes() {
        return getArgumentTypes(getDescriptor());
    }

    public static Type[] getArgumentTypes(final String methodDescriptor) {
        // First step: compute the number of argument types in methodDescriptor.
        int numArgumentTypes = 0;
        // Skip the first character, which is always a '('.
        int currentOffset = 1;
        // Parse the argument types, one at a each loop iteration.
        while (methodDescriptor.charAt(currentOffset) != ')') {
            while (methodDescriptor.charAt(currentOffset) == '[') {
                currentOffset++;
            }
            if (methodDescriptor.charAt(currentOffset++) == 'L') {
                // Skip the argument descriptor content.
                currentOffset = methodDescriptor.indexOf(';', currentOffset) + 1;
            }
            ++numArgumentTypes;
        }
        // Second step: create a Type instance for each argument type.
        Type[] argumentTypes = new Type[numArgumentTypes];
        // Skip the first character, which is always a '('.
        currentOffset = 1;
        // Parse and create the argument types, one at each loop iteration.
        int currentArgumentTypeIndex = 0;
        while (methodDescriptor.charAt(currentOffset) != ')') {
            final int currentArgumentTypeOffset = currentOffset;
            while (methodDescriptor.charAt(currentOffset) == '[') {
                currentOffset++;
            }
            if (methodDescriptor.charAt(currentOffset++) == 'L') {
                // Skip the argument descriptor content.
                currentOffset = methodDescriptor.indexOf(';', currentOffset) + 1;
            }
            argumentTypes[currentArgumentTypeIndex++] = getTypeInternal(methodDescriptor, currentArgumentTypeOffset, currentOffset);
        }
        return argumentTypes;
    }

    public static Type[] getArgumentTypes(final Method method) {
        Class<?>[] classes = method.getParameterTypes();
        Type[] types = new Type[classes.length];
        for (int i = classes.length - 1; i >= 0; --i) {
            types[i] = getType(classes[i]);
        }
        return types;
    }

    public Type getReturnType() {
        return getReturnType(getDescriptor());
    }

    public static Type getReturnType(final String methodDescriptor) {
        return getTypeInternal(methodDescriptor, getReturnTypeOffset(methodDescriptor), methodDescriptor.length());
    }

    public static Type getReturnType(final Method method) {
        return getType(method.getReturnType());
    }

    static int getReturnTypeOffset(final String methodDescriptor) {
        // Skip the first character, which is always a '('.
        int currentOffset = 1;
        // Skip the argument types, one at a each loop iteration.
        while (methodDescriptor.charAt(currentOffset) != ')') {
            while (methodDescriptor.charAt(currentOffset) == '[') {
                currentOffset++;
            }
            if (methodDescriptor.charAt(currentOffset++) == 'L') {
                // Skip the argument descriptor content.
                currentOffset = methodDescriptor.indexOf(';', currentOffset) + 1;
            }
        }
        return currentOffset + 1;
    }

    private static Type getTypeInternal(final String descriptorBuffer, final int descriptorBegin, final int descriptorEnd) {
        switch (descriptorBuffer.charAt(descriptorBegin)) {
            case 'V':
                return VOID_TYPE;
            case 'Z':
                return BOOLEAN_TYPE;
            case 'C':
                return CHAR_TYPE;
            case 'B':
                return BYTE_TYPE;
            case 'S':
                return SHORT_TYPE;
            case 'I':
                return INT_TYPE;
            case 'F':
                return FLOAT_TYPE;
            case 'J':
                return LONG_TYPE;
            case 'D':
                return DOUBLE_TYPE;
            case '[':
                return new Type(ARRAY, descriptorBuffer, descriptorBegin, descriptorEnd);
            case 'L':
                return new Type(OBJECT, descriptorBuffer, descriptorBegin + 1, descriptorEnd - 1);
            case '(':
                return new Type(METHOD, descriptorBuffer, descriptorBegin, descriptorEnd);
            default:
                throw new IllegalArgumentException();
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Methods to get class names, internal names or descriptors.
    // -----------------------------------------------------------------------------------------------

    public String getClassName() {
        switch (sort) {
            case VOID:
                return "void";
            case BOOLEAN:
                return "boolean";
            case CHAR:
                return "char";
            case BYTE:
                return "byte";
            case SHORT:
                return "short";
            case INT:
                return "int";
            case FLOAT:
                return "float";
            case LONG:
                return "long";
            case DOUBLE:
                return "double";
            case ARRAY:
                StringBuilder stringBuilder = new StringBuilder(getElementType().getClassName());
                for (int i = getDimensions(); i > 0; --i) {
                    stringBuilder.append("[]");
                }
                return stringBuilder.toString();
            case OBJECT:
            case INTERNAL:
                return valueBuffer.substring(valueBegin, valueEnd).replace('/', '.');
            default:
                throw new AssertionError();
        }
    }

    public String getInternalName() {
        return valueBuffer.substring(valueBegin, valueEnd);
    }

    public static String getInternalName(final Class<?> clazz) {
        return clazz.getName().replace('.', '/');
    }

    public String getDescriptor() {
        if (sort == OBJECT) {
            return valueBuffer.substring(valueBegin - 1, valueEnd + 1);
        } else if (sort == INTERNAL) {
            return 'L' + valueBuffer.substring(valueBegin, valueEnd) + ';';
        } else {
            return valueBuffer.substring(valueBegin, valueEnd);
        }
    }

    public static String getDescriptor(final Class<?> clazz) {
        StringBuilder stringBuilder = new StringBuilder();
        appendDescriptor(clazz, stringBuilder);
        return stringBuilder.toString();
    }

    public static String getConstructorDescriptor(final Constructor<?> constructor) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('(');
        Class<?>[] parameters = constructor.getParameterTypes();
        for (Class<?> parameter : parameters) {
            appendDescriptor(parameter, stringBuilder);
        }
        return stringBuilder.append(")V").toString();
    }

    public static String getMethodDescriptor(final Type returnType, final Type... argumentTypes) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('(');
        for (Type argumentType : argumentTypes) {
            argumentType.appendDescriptor(stringBuilder);
        }
        stringBuilder.append(')');
        returnType.appendDescriptor(stringBuilder);
        return stringBuilder.toString();
    }

    public static String getMethodDescriptor(final Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (Class<?> parameter : parameters) {
            appendDescriptor(parameter, stringBuilder);
        }
        stringBuilder.append(')');
        appendDescriptor(method.getReturnType(), stringBuilder);
        return stringBuilder.toString();
    }

    private void appendDescriptor(final StringBuilder stringBuilder) {
        if (sort == OBJECT) {
            stringBuilder.append(valueBuffer, valueBegin - 1, valueEnd + 1);
        } else if (sort == INTERNAL) {
            stringBuilder.append('L').append(valueBuffer, valueBegin, valueEnd).append(';');
        } else {
            stringBuilder.append(valueBuffer, valueBegin, valueEnd);
        }
    }

    private static void appendDescriptor(final Class<?> clazz, final StringBuilder stringBuilder) {
        Class<?> currentClass = clazz;
        while (currentClass.isArray()) {
            stringBuilder.append('[');
            currentClass = currentClass.getComponentType();
        }
        if (currentClass.isPrimitive()) {
            char descriptor;
            if (currentClass == Integer.TYPE) {
                descriptor = 'I';
            } else if (currentClass == Void.TYPE) {
                descriptor = 'V';
            } else if (currentClass == Boolean.TYPE) {
                descriptor = 'Z';
            } else if (currentClass == Byte.TYPE) {
                descriptor = 'B';
            } else if (currentClass == Character.TYPE) {
                descriptor = 'C';
            } else if (currentClass == Short.TYPE) {
                descriptor = 'S';
            } else if (currentClass == Double.TYPE) {
                descriptor = 'D';
            } else if (currentClass == Float.TYPE) {
                descriptor = 'F';
            } else if (currentClass == Long.TYPE) {
                descriptor = 'J';
            } else {
                throw new AssertionError();
            }
            stringBuilder.append(descriptor);
        } else {
            stringBuilder.append('L').append(getInternalName(currentClass)).append(';');
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Methods to get the sort, dimension, size, and opcodes corresponding to a Type or descriptor.
    // -----------------------------------------------------------------------------------------------

    public int getSort() {
        return sort == INTERNAL ? OBJECT : sort;
    }

    public int getDimensions() {
        int numDimensions = 1;
        while (valueBuffer.charAt(valueBegin + numDimensions) == '[') {
            numDimensions++;
        }
        return numDimensions;
    }

    public int getSize() {
        switch (sort) {
            case VOID:
                return 0;
            case BOOLEAN:
            case CHAR:
            case BYTE:
            case SHORT:
            case INT:
            case FLOAT:
            case ARRAY:
            case OBJECT:
            case INTERNAL:
                return 1;
            case LONG:
            case DOUBLE:
                return 2;
            default:
                throw new AssertionError();
        }
    }

    public int getArgumentsAndReturnSizes() {
        return getArgumentsAndReturnSizes(getDescriptor());
    }

    public static int getArgumentsAndReturnSizes(final String methodDescriptor) {
        int argumentsSize = 1;
        // Skip the first character, which is always a '('.
        int currentOffset = 1;
        int currentChar = methodDescriptor.charAt(currentOffset);
        // Parse the argument types and compute their size, one at a each loop iteration.
        while (currentChar != ')') {
            if (currentChar == 'J' || currentChar == 'D') {
                currentOffset++;
                argumentsSize += 2;
            } else {
                while (methodDescriptor.charAt(currentOffset) == '[') {
                    currentOffset++;
                }
                if (methodDescriptor.charAt(currentOffset++) == 'L') {
                    // Skip the argument descriptor content.
                    currentOffset = methodDescriptor.indexOf(';', currentOffset) + 1;
                }
                argumentsSize += 1;
            }
            currentChar = methodDescriptor.charAt(currentOffset);
        }
        currentChar = methodDescriptor.charAt(currentOffset + 1);
        if (currentChar == 'V') {
            return argumentsSize << 2;
        } else {
            int returnSize = (currentChar == 'J' || currentChar == 'D') ? 2 : 1;
            return argumentsSize << 2 | returnSize;
        }
    }

    public int getOpcode(final int opcode) {
        if (opcode == Opcodes.IALOAD || opcode == Opcodes.IASTORE) {
            switch (sort) {
                case BOOLEAN:
                case BYTE:
                    return opcode + (Opcodes.BALOAD - Opcodes.IALOAD);
                case CHAR:
                    return opcode + (Opcodes.CALOAD - Opcodes.IALOAD);
                case SHORT:
                    return opcode + (Opcodes.SALOAD - Opcodes.IALOAD);
                case INT:
                    return opcode;
                case FLOAT:
                    return opcode + (Opcodes.FALOAD - Opcodes.IALOAD);
                case LONG:
                    return opcode + (Opcodes.LALOAD - Opcodes.IALOAD);
                case DOUBLE:
                    return opcode + (Opcodes.DALOAD - Opcodes.IALOAD);
                case ARRAY:
                case OBJECT:
                case INTERNAL:
                    return opcode + (Opcodes.AALOAD - Opcodes.IALOAD);
                case METHOD:
                case VOID:
                    throw new UnsupportedOperationException();
                default:
                    throw new AssertionError();
            }
        } else {
            switch (sort) {
                case VOID:
                    if (opcode != Opcodes.IRETURN) {
                        throw new UnsupportedOperationException();
                    }
                    return Opcodes.RETURN;
                case BOOLEAN:
                case BYTE:
                case CHAR:
                case SHORT:
                case INT:
                    return opcode;
                case FLOAT:
                    return opcode + (Opcodes.FRETURN - Opcodes.IRETURN);
                case LONG:
                    return opcode + (Opcodes.LRETURN - Opcodes.IRETURN);
                case DOUBLE:
                    return opcode + (Opcodes.DRETURN - Opcodes.IRETURN);
                case ARRAY:
                case OBJECT:
                case INTERNAL:
                    if (opcode != Opcodes.ILOAD && opcode != Opcodes.ISTORE && opcode != Opcodes.IRETURN) {
                        throw new UnsupportedOperationException();
                    }
                    return opcode + (Opcodes.ARETURN - Opcodes.IRETURN);
                case METHOD:
                    throw new UnsupportedOperationException();
                default:
                    throw new AssertionError();
            }
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Equals, hashCode and toString.
    // -----------------------------------------------------------------------------------------------

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Type)) {
            return false;
        }
        Type other = (Type) object;
        if ((sort == INTERNAL ? OBJECT : sort) != (other.sort == INTERNAL ? OBJECT : other.sort)) {
            return false;
        }
        int begin = valueBegin;
        int end = valueEnd;
        int otherBegin = other.valueBegin;
        int otherEnd = other.valueEnd;
        // Compare the values.
        if (end - begin != otherEnd - otherBegin) {
            return false;
        }
        for (int i = begin, j = otherBegin; i < end; i++, j++) {
            if (valueBuffer.charAt(i) != other.valueBuffer.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 13 * (sort == INTERNAL ? OBJECT : sort);
        if (sort >= ARRAY) {
            for (int i = valueBegin, end = valueEnd; i < end; i++) {
                hashCode = 17 * (hashCode + valueBuffer.charAt(i));
            }
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return getDescriptor();
    }

}
