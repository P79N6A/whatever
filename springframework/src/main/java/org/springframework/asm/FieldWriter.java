package org.springframework.asm;

final class FieldWriter extends FieldVisitor {

    private final SymbolTable symbolTable;
    // Note: fields are ordered as in the field_info structure, and those related to attributes are
    // ordered as in Section 4.7 of the JVMS.

    private final int accessFlags;

    private final int nameIndex;

    private final int descriptorIndex;

    private int signatureIndex;

    private int constantValueIndex;

    private AnnotationWriter lastRuntimeVisibleAnnotation;

    private AnnotationWriter lastRuntimeInvisibleAnnotation;

    private AnnotationWriter lastRuntimeVisibleTypeAnnotation;

    private AnnotationWriter lastRuntimeInvisibleTypeAnnotation;

    private Attribute firstAttribute;
    // -----------------------------------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------------------------------

    FieldWriter(final SymbolTable symbolTable, final int access, final String name, final String descriptor, final String signature, final Object constantValue) {
        super(Opcodes.ASM7);
        this.symbolTable = symbolTable;
        this.accessFlags = access;
        this.nameIndex = symbolTable.addConstantUtf8(name);
        this.descriptorIndex = symbolTable.addConstantUtf8(descriptor);
        if (signature != null) {
            this.signatureIndex = symbolTable.addConstantUtf8(signature);
        }
        if (constantValue != null) {
            this.constantValueIndex = symbolTable.addConstant(constantValue).index;
        }
    }
    // -----------------------------------------------------------------------------------------------
    // Implementation of the FieldVisitor abstract class
    // -----------------------------------------------------------------------------------------------

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
        if (visible) {
            return lastRuntimeVisibleAnnotation = AnnotationWriter.create(symbolTable, descriptor, lastRuntimeVisibleAnnotation);
        } else {
            return lastRuntimeInvisibleAnnotation = AnnotationWriter.create(symbolTable, descriptor, lastRuntimeInvisibleAnnotation);
        }
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String descriptor, final boolean visible) {
        if (visible) {
            return lastRuntimeVisibleTypeAnnotation = AnnotationWriter.create(symbolTable, typeRef, typePath, descriptor, lastRuntimeVisibleTypeAnnotation);
        } else {
            return lastRuntimeInvisibleTypeAnnotation = AnnotationWriter.create(symbolTable, typeRef, typePath, descriptor, lastRuntimeInvisibleTypeAnnotation);
        }
    }

    @Override
    public void visitAttribute(final Attribute attribute) {
        // Store the attributes in the <i>reverse</i> order of their visit by this method.
        attribute.nextAttribute = firstAttribute;
        firstAttribute = attribute;
    }

    @Override
    public void visitEnd() {
        // Nothing to do.
    }
    // -----------------------------------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------------------------------

    int computeFieldInfoSize() {
        // The access_flags, name_index, descriptor_index and attributes_count fields use 8 bytes.
        int size = 8;
        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        if (constantValueIndex != 0) {
            // ConstantValue attributes always use 8 bytes.
            symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE);
            size += 8;
        }
        size += Attribute.computeAttributesSize(symbolTable, accessFlags, signatureIndex);
        size += AnnotationWriter.computeAnnotationsSize(lastRuntimeVisibleAnnotation, lastRuntimeInvisibleAnnotation, lastRuntimeVisibleTypeAnnotation, lastRuntimeInvisibleTypeAnnotation);
        if (firstAttribute != null) {
            size += firstAttribute.computeAttributesSize(symbolTable);
        }
        return size;
    }

    void putFieldInfo(final ByteVector output) {
        boolean useSyntheticAttribute = symbolTable.getMajorVersion() < Opcodes.V1_5;
        // Put the access_flags, name_index and descriptor_index fields.
        int mask = useSyntheticAttribute ? Opcodes.ACC_SYNTHETIC : 0;
        output.putShort(accessFlags & ~mask).putShort(nameIndex).putShort(descriptorIndex);
        // Compute and put the attributes_count field.
        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        int attributesCount = 0;
        if (constantValueIndex != 0) {
            ++attributesCount;
        }
        if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && useSyntheticAttribute) {
            ++attributesCount;
        }
        if (signatureIndex != 0) {
            ++attributesCount;
        }
        if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
            ++attributesCount;
        }
        if (lastRuntimeVisibleAnnotation != null) {
            ++attributesCount;
        }
        if (lastRuntimeInvisibleAnnotation != null) {
            ++attributesCount;
        }
        if (lastRuntimeVisibleTypeAnnotation != null) {
            ++attributesCount;
        }
        if (lastRuntimeInvisibleTypeAnnotation != null) {
            ++attributesCount;
        }
        if (firstAttribute != null) {
            attributesCount += firstAttribute.getAttributeCount();
        }
        output.putShort(attributesCount);
        // Put the field_info attributes.
        // For ease of reference, we use here the same attribute order as in Section 4.7 of the JVMS.
        if (constantValueIndex != 0) {
            output.putShort(symbolTable.addConstantUtf8(Constants.CONSTANT_VALUE)).putInt(2).putShort(constantValueIndex);
        }
        Attribute.putAttributes(symbolTable, accessFlags, signatureIndex, output);
        AnnotationWriter.putAnnotations(symbolTable, lastRuntimeVisibleAnnotation, lastRuntimeInvisibleAnnotation, lastRuntimeVisibleTypeAnnotation, lastRuntimeInvisibleTypeAnnotation, output);
        if (firstAttribute != null) {
            firstAttribute.putAttributes(symbolTable, output);
        }
    }

    final void collectAttributePrototypes(final Attribute.Set attributePrototypes) {
        attributePrototypes.addAttributes(firstAttribute);
    }

}
