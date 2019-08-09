package org.springframework.asm;

public class TypeReference {

    public static final int CLASS_TYPE_PARAMETER = 0x00;

    public static final int METHOD_TYPE_PARAMETER = 0x01;

    public static final int CLASS_EXTENDS = 0x10;

    public static final int CLASS_TYPE_PARAMETER_BOUND = 0x11;

    public static final int METHOD_TYPE_PARAMETER_BOUND = 0x12;

    public static final int FIELD = 0x13;

    public static final int METHOD_RETURN = 0x14;

    public static final int METHOD_RECEIVER = 0x15;

    public static final int METHOD_FORMAL_PARAMETER = 0x16;

    public static final int THROWS = 0x17;

    public static final int LOCAL_VARIABLE = 0x40;

    public static final int RESOURCE_VARIABLE = 0x41;

    public static final int EXCEPTION_PARAMETER = 0x42;

    public static final int INSTANCEOF = 0x43;

    public static final int NEW = 0x44;

    public static final int CONSTRUCTOR_REFERENCE = 0x45;

    public static final int METHOD_REFERENCE = 0x46;

    public static final int CAST = 0x47;

    public static final int CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT = 0x48;

    public static final int METHOD_INVOCATION_TYPE_ARGUMENT = 0x49;

    public static final int CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT = 0x4A;

    public static final int METHOD_REFERENCE_TYPE_ARGUMENT = 0x4B;

    private final int targetTypeAndInfo;

    public TypeReference(final int typeRef) {
        this.targetTypeAndInfo = typeRef;
    }

    public static TypeReference newTypeReference(final int sort) {
        return new TypeReference(sort << 24);
    }

    public static TypeReference newTypeParameterReference(final int sort, final int paramIndex) {
        return new TypeReference((sort << 24) | (paramIndex << 16));
    }

    public static TypeReference newTypeParameterBoundReference(final int sort, final int paramIndex, final int boundIndex) {
        return new TypeReference((sort << 24) | (paramIndex << 16) | (boundIndex << 8));
    }

    public static TypeReference newSuperTypeReference(final int itfIndex) {
        return new TypeReference((CLASS_EXTENDS << 24) | ((itfIndex & 0xFFFF) << 8));
    }

    public static TypeReference newFormalParameterReference(final int paramIndex) {
        return new TypeReference((METHOD_FORMAL_PARAMETER << 24) | (paramIndex << 16));
    }

    public static TypeReference newExceptionReference(final int exceptionIndex) {
        return new TypeReference((THROWS << 24) | (exceptionIndex << 8));
    }

    public static TypeReference newTryCatchReference(final int tryCatchBlockIndex) {
        return new TypeReference((EXCEPTION_PARAMETER << 24) | (tryCatchBlockIndex << 8));
    }

    public static TypeReference newTypeArgumentReference(final int sort, final int argIndex) {
        return new TypeReference((sort << 24) | argIndex);
    }

    public int getSort() {
        return targetTypeAndInfo >>> 24;
    }

    public int getTypeParameterIndex() {
        return (targetTypeAndInfo & 0x00FF0000) >> 16;
    }

    public int getTypeParameterBoundIndex() {
        return (targetTypeAndInfo & 0x0000FF00) >> 8;
    }

    public int getSuperTypeIndex() {
        return (short) ((targetTypeAndInfo & 0x00FFFF00) >> 8);
    }

    public int getFormalParameterIndex() {
        return (targetTypeAndInfo & 0x00FF0000) >> 16;
    }

    public int getExceptionIndex() {
        return (targetTypeAndInfo & 0x00FFFF00) >> 8;
    }

    public int getTryCatchBlockIndex() {
        return (targetTypeAndInfo & 0x00FFFF00) >> 8;
    }

    public int getTypeArgumentIndex() {
        return targetTypeAndInfo & 0xFF;
    }

    public int getValue() {
        return targetTypeAndInfo;
    }

    static void putTarget(final int targetTypeAndInfo, final ByteVector output) {
        switch (targetTypeAndInfo >>> 24) {
            case CLASS_TYPE_PARAMETER:
            case METHOD_TYPE_PARAMETER:
            case METHOD_FORMAL_PARAMETER:
                output.putShort(targetTypeAndInfo >>> 16);
                break;
            case FIELD:
            case METHOD_RETURN:
            case METHOD_RECEIVER:
                output.putByte(targetTypeAndInfo >>> 24);
                break;
            case CAST:
            case CONSTRUCTOR_INVOCATION_TYPE_ARGUMENT:
            case METHOD_INVOCATION_TYPE_ARGUMENT:
            case CONSTRUCTOR_REFERENCE_TYPE_ARGUMENT:
            case METHOD_REFERENCE_TYPE_ARGUMENT:
                output.putInt(targetTypeAndInfo);
                break;
            case CLASS_EXTENDS:
            case CLASS_TYPE_PARAMETER_BOUND:
            case METHOD_TYPE_PARAMETER_BOUND:
            case THROWS:
            case EXCEPTION_PARAMETER:
            case INSTANCEOF:
            case NEW:
            case CONSTRUCTOR_REFERENCE:
            case METHOD_REFERENCE:
                output.put12(targetTypeAndInfo >>> 24, (targetTypeAndInfo & 0xFFFF00) >> 8);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

}
