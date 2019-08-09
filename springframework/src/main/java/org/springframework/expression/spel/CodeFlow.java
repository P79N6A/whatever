package org.springframework.expression.spel;

import org.springframework.asm.ClassWriter;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class CodeFlow implements Opcodes {

    private final String className;

    private final ClassWriter classWriter;

    private final Deque<List<String>> compilationScopes;

    @Nullable
    private List<FieldAdder> fieldAdders;

    @Nullable
    private List<ClinitAdder> clinitAdders;

    private int nextFieldId = 1;

    private int nextFreeVariableId = 1;

    public CodeFlow(String className, ClassWriter classWriter) {
        this.className = className;
        this.classWriter = classWriter;
        this.compilationScopes = new ArrayDeque<>();
        this.compilationScopes.add(new ArrayList<String>());
    }

    public void loadTarget(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 1);
    }

    public void loadEvaluationContext(MethodVisitor mv) {
        mv.visitVarInsn(ALOAD, 2);
    }

    public void pushDescriptor(@Nullable String descriptor) {
        if (descriptor != null) {
            this.compilationScopes.element().add(descriptor);
        }
    }

    public void enterCompilationScope() {
        this.compilationScopes.push(new ArrayList<>());
    }

    public void exitCompilationScope() {
        this.compilationScopes.pop();
    }

    @Nullable
    public String lastDescriptor() {
        return CollectionUtils.lastElement(this.compilationScopes.peek());
    }

    public void unboxBooleanIfNecessary(MethodVisitor mv) {
        if ("Ljava/lang/Boolean".equals(lastDescriptor())) {
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        }
    }

    public void finish() {
        if (this.fieldAdders != null) {
            for (FieldAdder fieldAdder : this.fieldAdders) {
                fieldAdder.generateField(this.classWriter, this);
            }
        }
        if (this.clinitAdders != null) {
            MethodVisitor mv = this.classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            this.nextFreeVariableId = 0;  // to 0 because there is no 'this' in a clinit
            for (ClinitAdder clinitAdder : this.clinitAdders) {
                clinitAdder.generateCode(mv, this);
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(0, 0);  // not supplied due to COMPUTE_MAXS
            mv.visitEnd();
        }
    }

    public void registerNewField(FieldAdder fieldAdder) {
        if (this.fieldAdders == null) {
            this.fieldAdders = new ArrayList<>();
        }
        this.fieldAdders.add(fieldAdder);
    }

    public void registerNewClinit(ClinitAdder clinitAdder) {
        if (this.clinitAdders == null) {
            this.clinitAdders = new ArrayList<>();
        }
        this.clinitAdders.add(clinitAdder);
    }

    public int nextFieldId() {
        return this.nextFieldId++;
    }

    public int nextFreeVariableId() {
        return this.nextFreeVariableId++;
    }

    public String getClassName() {
        return this.className;
    }

    public static void insertUnboxInsns(MethodVisitor mv, char ch, @Nullable String stackDescriptor) {
        if (stackDescriptor == null) {
            return;
        }
        switch (ch) {
            case 'Z':
                if (!stackDescriptor.equals("Ljava/lang/Boolean")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                break;
            case 'B':
                if (!stackDescriptor.equals("Ljava/lang/Byte")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                break;
            case 'C':
                if (!stackDescriptor.equals("Ljava/lang/Character")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                break;
            case 'D':
                if (!stackDescriptor.equals("Ljava/lang/Double")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                break;
            case 'F':
                if (!stackDescriptor.equals("Ljava/lang/Float")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                break;
            case 'I':
                if (!stackDescriptor.equals("Ljava/lang/Integer")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                break;
            case 'J':
                if (!stackDescriptor.equals("Ljava/lang/Long")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                break;
            case 'S':
                if (!stackDescriptor.equals("Ljava/lang/Short")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                break;
            default:
                throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + ch + "'");
        }
    }

    public static void insertUnboxNumberInsns(MethodVisitor mv, char targetDescriptor, @Nullable String stackDescriptor) {
        if (stackDescriptor == null) {
            return;
        }
        switch (targetDescriptor) {
            case 'D':
                if (stackDescriptor.equals("Ljava/lang/Object")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
                break;
            case 'F':
                if (stackDescriptor.equals("Ljava/lang/Object")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
                break;
            case 'J':
                if (stackDescriptor.equals("Ljava/lang/Object")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
                break;
            case 'I':
                if (stackDescriptor.equals("Ljava/lang/Object")) {
                    mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
                }
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
                break;
            // does not handle Z, B, C, S
            default:
                throw new IllegalArgumentException("Unboxing should not be attempted for descriptor '" + targetDescriptor + "'");
        }
    }

    public static void insertAnyNecessaryTypeConversionBytecodes(MethodVisitor mv, char targetDescriptor, String stackDescriptor) {
        if (CodeFlow.isPrimitive(stackDescriptor)) {
            char stackTop = stackDescriptor.charAt(0);
            if (stackTop == 'I' || stackTop == 'B' || stackTop == 'S' || stackTop == 'C') {
                if (targetDescriptor == 'D') {
                    mv.visitInsn(I2D);
                } else if (targetDescriptor == 'F') {
                    mv.visitInsn(I2F);
                } else if (targetDescriptor == 'J') {
                    mv.visitInsn(I2L);
                } else if (targetDescriptor == 'I') {
                    // nop
                } else {
                    throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
                }
            } else if (stackTop == 'J') {
                if (targetDescriptor == 'D') {
                    mv.visitInsn(L2D);
                } else if (targetDescriptor == 'F') {
                    mv.visitInsn(L2F);
                } else if (targetDescriptor == 'J') {
                    // nop
                } else if (targetDescriptor == 'I') {
                    mv.visitInsn(L2I);
                } else {
                    throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
                }
            } else if (stackTop == 'F') {
                if (targetDescriptor == 'D') {
                    mv.visitInsn(F2D);
                } else if (targetDescriptor == 'F') {
                    // nop
                } else if (targetDescriptor == 'J') {
                    mv.visitInsn(F2L);
                } else if (targetDescriptor == 'I') {
                    mv.visitInsn(F2I);
                } else {
                    throw new IllegalStateException("Cannot get from " + stackTop + " to " + targetDescriptor);
                }
            } else if (stackTop == 'D') {
                if (targetDescriptor == 'D') {
                    // nop
                } else if (targetDescriptor == 'F') {
                    mv.visitInsn(D2F);
                } else if (targetDescriptor == 'J') {
                    mv.visitInsn(D2L);
                } else if (targetDescriptor == 'I') {
                    mv.visitInsn(D2I);
                } else {
                    throw new IllegalStateException("Cannot get from " + stackDescriptor + " to " + targetDescriptor);
                }
            }
        }
    }

    public static String createSignatureDescriptor(Method method) {
        Class<?>[] params = method.getParameterTypes();
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (Class<?> param : params) {
            sb.append(toJvmDescriptor(param));
        }
        sb.append(")");
        sb.append(toJvmDescriptor(method.getReturnType()));
        return sb.toString();
    }

    public static String createSignatureDescriptor(Constructor<?> ctor) {
        Class<?>[] params = ctor.getParameterTypes();
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (Class<?> param : params) {
            sb.append(toJvmDescriptor(param));
        }
        sb.append(")V");
        return sb.toString();
    }

    public static String toJvmDescriptor(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        if (clazz.isArray()) {
            while (clazz.isArray()) {
                sb.append("[");
                clazz = clazz.getComponentType();
            }
        }
        if (clazz.isPrimitive()) {
            if (clazz == Boolean.TYPE) {
                sb.append('Z');
            } else if (clazz == Byte.TYPE) {
                sb.append('B');
            } else if (clazz == Character.TYPE) {
                sb.append('C');
            } else if (clazz == Double.TYPE) {
                sb.append('D');
            } else if (clazz == Float.TYPE) {
                sb.append('F');
            } else if (clazz == Integer.TYPE) {
                sb.append('I');
            } else if (clazz == Long.TYPE) {
                sb.append('J');
            } else if (clazz == Short.TYPE) {
                sb.append('S');
            } else if (clazz == Void.TYPE) {
                sb.append('V');
            }
        } else {
            sb.append("L");
            sb.append(clazz.getName().replace('.', '/'));
            sb.append(";");
        }
        return sb.toString();
    }

    public static String toDescriptorFromObject(@Nullable Object value) {
        if (value == null) {
            return "Ljava/lang/Object";
        } else {
            return toDescriptor(value.getClass());
        }
    }

    public static boolean isBooleanCompatible(@Nullable String descriptor) {
        return (descriptor != null && (descriptor.equals("Z") || descriptor.equals("Ljava/lang/Boolean")));
    }

    public static boolean isPrimitive(@Nullable String descriptor) {
        return (descriptor != null && descriptor.length() == 1);
    }

    public static boolean isPrimitiveArray(@Nullable String descriptor) {
        if (descriptor == null) {
            return false;
        }
        boolean primitive = true;
        for (int i = 0, max = descriptor.length(); i < max; i++) {
            char ch = descriptor.charAt(i);
            if (ch == '[') {
                continue;
            }
            primitive = (ch != 'L');
            break;
        }
        return primitive;
    }

    public static boolean areBoxingCompatible(String desc1, String desc2) {
        if (desc1.equals(desc2)) {
            return true;
        }
        if (desc1.length() == 1) {
            if (desc1.equals("Z")) {
                return desc2.equals("Ljava/lang/Boolean");
            } else if (desc1.equals("D")) {
                return desc2.equals("Ljava/lang/Double");
            } else if (desc1.equals("F")) {
                return desc2.equals("Ljava/lang/Float");
            } else if (desc1.equals("I")) {
                return desc2.equals("Ljava/lang/Integer");
            } else if (desc1.equals("J")) {
                return desc2.equals("Ljava/lang/Long");
            }
        } else if (desc2.length() == 1) {
            if (desc2.equals("Z")) {
                return desc1.equals("Ljava/lang/Boolean");
            } else if (desc2.equals("D")) {
                return desc1.equals("Ljava/lang/Double");
            } else if (desc2.equals("F")) {
                return desc1.equals("Ljava/lang/Float");
            } else if (desc2.equals("I")) {
                return desc1.equals("Ljava/lang/Integer");
            } else if (desc2.equals("J")) {
                return desc1.equals("Ljava/lang/Long");
            }
        }
        return false;
    }

    public static boolean isPrimitiveOrUnboxableSupportedNumberOrBoolean(@Nullable String descriptor) {
        if (descriptor == null) {
            return false;
        }
        if (isPrimitiveOrUnboxableSupportedNumber(descriptor)) {
            return true;
        }
        return ("Z".equals(descriptor) || descriptor.equals("Ljava/lang/Boolean"));
    }

    public static boolean isPrimitiveOrUnboxableSupportedNumber(@Nullable String descriptor) {
        if (descriptor == null) {
            return false;
        }
        if (descriptor.length() == 1) {
            return "DFIJ".contains(descriptor);
        }
        if (descriptor.startsWith("Ljava/lang/")) {
            String name = descriptor.substring("Ljava/lang/".length());
            if (name.equals("Double") || name.equals("Float") || name.equals("Integer") || name.equals("Long")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isIntegerForNumericOp(Number number) {
        return (number instanceof Integer || number instanceof Short || number instanceof Byte);
    }

    public static char toPrimitiveTargetDesc(String descriptor) {
        if (descriptor.length() == 1) {
            return descriptor.charAt(0);
        } else if (descriptor.equals("Ljava/lang/Boolean")) {
            return 'Z';
        } else if (descriptor.equals("Ljava/lang/Byte")) {
            return 'B';
        } else if (descriptor.equals("Ljava/lang/Character")) {
            return 'C';
        } else if (descriptor.equals("Ljava/lang/Double")) {
            return 'D';
        } else if (descriptor.equals("Ljava/lang/Float")) {
            return 'F';
        } else if (descriptor.equals("Ljava/lang/Integer")) {
            return 'I';
        } else if (descriptor.equals("Ljava/lang/Long")) {
            return 'J';
        } else if (descriptor.equals("Ljava/lang/Short")) {
            return 'S';
        } else {
            throw new IllegalStateException("No primitive for '" + descriptor + "'");
        }
    }

    public static void insertCheckCast(MethodVisitor mv, @Nullable String descriptor) {
        if (descriptor != null && descriptor.length() != 1) {
            if (descriptor.charAt(0) == '[') {
                if (isPrimitiveArray(descriptor)) {
                    mv.visitTypeInsn(CHECKCAST, descriptor);
                } else {
                    mv.visitTypeInsn(CHECKCAST, descriptor + ";");
                }
            } else {
                if (!descriptor.equals("Ljava/lang/Object")) {
                    // This is chopping off the 'L' to leave us with "java/lang/String"
                    mv.visitTypeInsn(CHECKCAST, descriptor.substring(1));
                }
            }
        }
    }

    public static void insertBoxIfNecessary(MethodVisitor mv, @Nullable String descriptor) {
        if (descriptor != null && descriptor.length() == 1) {
            insertBoxIfNecessary(mv, descriptor.charAt(0));
        }
    }

    public static void insertBoxIfNecessary(MethodVisitor mv, char ch) {
        switch (ch) {
            case 'Z':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case 'B':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case 'C':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case 'D':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case 'F':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case 'I':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case 'J':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case 'S':
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                break;
            case 'L':
            case 'V':
            case '[':
                // no box needed
                break;
            default:
                throw new IllegalArgumentException("Boxing should not be attempted for descriptor '" + ch + "'");
        }
    }

    public static String toDescriptor(Class<?> type) {
        String name = type.getName();
        if (type.isPrimitive()) {
            switch (name.length()) {
                case 3:
                    return "I";
                case 4:
                    if (name.equals("byte")) {
                        return "B";
                    } else if (name.equals("char")) {
                        return "C";
                    } else if (name.equals("long")) {
                        return "J";
                    } else if (name.equals("void")) {
                        return "V";
                    }
                    break;
                case 5:
                    if (name.equals("float")) {
                        return "F";
                    } else if (name.equals("short")) {
                        return "S";
                    }
                    break;
                case 6:
                    if (name.equals("double")) {
                        return "D";
                    }
                    break;
                case 7:
                    if (name.equals("boolean")) {
                        return "Z";
                    }
                    break;
            }
        } else {
            if (name.charAt(0) != '[') {
                return "L" + type.getName().replace('.', '/');
            } else {
                if (name.endsWith(";")) {
                    return name.substring(0, name.length() - 1).replace('.', '/');
                } else {
                    return name;  // array has primitive component type
                }
            }
        }
        return "";
    }

    public static String[] toParamDescriptors(Method method) {
        return toDescriptors(method.getParameterTypes());
    }

    public static String[] toParamDescriptors(Constructor<?> ctor) {
        return toDescriptors(ctor.getParameterTypes());
    }

    public static String[] toDescriptors(Class<?>[] types) {
        int typesCount = types.length;
        String[] descriptors = new String[typesCount];
        for (int p = 0; p < typesCount; p++) {
            descriptors[p] = toDescriptor(types[p]);
        }
        return descriptors;
    }

    public static void insertOptimalLoad(MethodVisitor mv, int value) {
        if (value < 6) {
            mv.visitInsn(ICONST_0 + value);
        } else if (value < Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, value);
        } else if (value < Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    public static void insertArrayStore(MethodVisitor mv, String arrayElementType) {
        if (arrayElementType.length() == 1) {
            switch (arrayElementType.charAt(0)) {
                case 'I':
                    mv.visitInsn(IASTORE);
                    break;
                case 'J':
                    mv.visitInsn(LASTORE);
                    break;
                case 'F':
                    mv.visitInsn(FASTORE);
                    break;
                case 'D':
                    mv.visitInsn(DASTORE);
                    break;
                case 'B':
                    mv.visitInsn(BASTORE);
                    break;
                case 'C':
                    mv.visitInsn(CASTORE);
                    break;
                case 'S':
                    mv.visitInsn(SASTORE);
                    break;
                case 'Z':
                    mv.visitInsn(BASTORE);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected arraytype " + arrayElementType.charAt(0));
            }
        } else {
            mv.visitInsn(AASTORE);
        }
    }

    public static int arrayCodeFor(String arraytype) {
        switch (arraytype.charAt(0)) {
            case 'I':
                return T_INT;
            case 'J':
                return T_LONG;
            case 'F':
                return T_FLOAT;
            case 'D':
                return T_DOUBLE;
            case 'B':
                return T_BYTE;
            case 'C':
                return T_CHAR;
            case 'S':
                return T_SHORT;
            case 'Z':
                return T_BOOLEAN;
            default:
                throw new IllegalArgumentException("Unexpected arraytype " + arraytype.charAt(0));
        }
    }

    public static boolean isReferenceTypeArray(String arraytype) {
        int length = arraytype.length();
        for (int i = 0; i < length; i++) {
            char ch = arraytype.charAt(i);
            if (ch == '[') {
                continue;
            }
            return (ch == 'L');
        }
        return false;
    }

    public static void insertNewArrayCode(MethodVisitor mv, int size, String arraytype) {
        insertOptimalLoad(mv, size);
        if (arraytype.length() == 1) {
            mv.visitIntInsn(NEWARRAY, CodeFlow.arrayCodeFor(arraytype));
        } else {
            if (arraytype.charAt(0) == '[') {
                // Handling the nested array case here.
                // If vararg is [[I then we want [I and not [I;
                if (CodeFlow.isReferenceTypeArray(arraytype)) {
                    mv.visitTypeInsn(ANEWARRAY, arraytype + ";");
                } else {
                    mv.visitTypeInsn(ANEWARRAY, arraytype);
                }
            } else {
                mv.visitTypeInsn(ANEWARRAY, arraytype.substring(1));
            }
        }
    }

    public static void insertNumericUnboxOrPrimitiveTypeCoercion(MethodVisitor mv, @Nullable String stackDescriptor, char targetDescriptor) {
        if (!CodeFlow.isPrimitive(stackDescriptor)) {
            CodeFlow.insertUnboxNumberInsns(mv, targetDescriptor, stackDescriptor);
        } else {
            CodeFlow.insertAnyNecessaryTypeConversionBytecodes(mv, targetDescriptor, stackDescriptor);
        }
    }

    public static String toBoxedDescriptor(String primitiveDescriptor) {
        switch (primitiveDescriptor.charAt(0)) {
            case 'I':
                return "Ljava/lang/Integer";
            case 'J':
                return "Ljava/lang/Long";
            case 'F':
                return "Ljava/lang/Float";
            case 'D':
                return "Ljava/lang/Double";
            case 'B':
                return "Ljava/lang/Byte";
            case 'C':
                return "Ljava/lang/Character";
            case 'S':
                return "Ljava/lang/Short";
            case 'Z':
                return "Ljava/lang/Boolean";
            default:
                throw new IllegalArgumentException("Unexpected non primitive descriptor " + primitiveDescriptor);
        }
    }

    @FunctionalInterface
    public interface FieldAdder {

        void generateField(ClassWriter cw, CodeFlow codeflow);

    }

    @FunctionalInterface
    public interface ClinitAdder {

        void generateCode(MethodVisitor mv, CodeFlow codeflow);

    }

}
