package org.springframework.asm;

public class Attribute {

    public final String type;

    private byte[] content;

    Attribute nextAttribute;

    protected Attribute(final String type) {
        this.type = type;
    }

    public boolean isUnknown() {
        return true;
    }

    public boolean isCodeAttribute() {
        return false;
    }

    protected Label[] getLabels() {
        return new Label[0];
    }

    protected Attribute read(final ClassReader classReader, final int offset, final int length, final char[] charBuffer, final int codeAttributeOffset, final Label[] labels) {
        Attribute attribute = new Attribute(type);
        attribute.content = new byte[length];
        System.arraycopy(classReader.classFileBuffer, offset, attribute.content, 0, length);
        return attribute;
    }

    protected ByteVector write(final ClassWriter classWriter, final byte[] code, final int codeLength, final int maxStack, final int maxLocals) {
        return new ByteVector(content);
    }

    final int getAttributeCount() {
        int count = 0;
        Attribute attribute = this;
        while (attribute != null) {
            count += 1;
            attribute = attribute.nextAttribute;
        }
        return count;
    }

    final int computeAttributesSize(final SymbolTable symbolTable) {
        final byte[] code = null;
        final int codeLength = 0;
        final int maxStack = -1;
        final int maxLocals = -1;
        return computeAttributesSize(symbolTable, code, codeLength, maxStack, maxLocals);
    }

    final int computeAttributesSize(final SymbolTable symbolTable, final byte[] code, final int codeLength, final int maxStack, final int maxLocals) {
        final ClassWriter classWriter = symbolTable.classWriter;
        int size = 0;
        Attribute attribute = this;
        while (attribute != null) {
            symbolTable.addConstantUtf8(attribute.type);
            size += 6 + attribute.write(classWriter, code, codeLength, maxStack, maxLocals).length;
            attribute = attribute.nextAttribute;
        }
        return size;
    }

    static int computeAttributesSize(final SymbolTable symbolTable, final int accessFlags, final int signatureIndex) {
        int size = 0;
        // Before Java 1.5, synthetic fields are represented with a Synthetic attribute.
        if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && symbolTable.getMajorVersion() < Opcodes.V1_5) {
            // Synthetic attributes always use 6 bytes.
            symbolTable.addConstantUtf8(Constants.SYNTHETIC);
            size += 6;
        }
        if (signatureIndex != 0) {
            // Signature attributes always use 8 bytes.
            symbolTable.addConstantUtf8(Constants.SIGNATURE);
            size += 8;
        }
        // ACC_DEPRECATED is ASM specific, the ClassFile format uses a Deprecated attribute instead.
        if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
            // Deprecated attributes always use 6 bytes.
            symbolTable.addConstantUtf8(Constants.DEPRECATED);
            size += 6;
        }
        return size;
    }

    final void putAttributes(final SymbolTable symbolTable, final ByteVector output) {
        final byte[] code = null;
        final int codeLength = 0;
        final int maxStack = -1;
        final int maxLocals = -1;
        putAttributes(symbolTable, code, codeLength, maxStack, maxLocals, output);
    }

    final void putAttributes(final SymbolTable symbolTable, final byte[] code, final int codeLength, final int maxStack, final int maxLocals, final ByteVector output) {
        final ClassWriter classWriter = symbolTable.classWriter;
        Attribute attribute = this;
        while (attribute != null) {
            ByteVector attributeContent = attribute.write(classWriter, code, codeLength, maxStack, maxLocals);
            // Put attribute_name_index and attribute_length.
            output.putShort(symbolTable.addConstantUtf8(attribute.type)).putInt(attributeContent.length);
            output.putByteArray(attributeContent.data, 0, attributeContent.length);
            attribute = attribute.nextAttribute;
        }
    }

    static void putAttributes(final SymbolTable symbolTable, final int accessFlags, final int signatureIndex, final ByteVector output) {
        // Before Java 1.5, synthetic fields are represented with a Synthetic attribute.
        if ((accessFlags & Opcodes.ACC_SYNTHETIC) != 0 && symbolTable.getMajorVersion() < Opcodes.V1_5) {
            output.putShort(symbolTable.addConstantUtf8(Constants.SYNTHETIC)).putInt(0);
        }
        if (signatureIndex != 0) {
            output.putShort(symbolTable.addConstantUtf8(Constants.SIGNATURE)).putInt(2).putShort(signatureIndex);
        }
        if ((accessFlags & Opcodes.ACC_DEPRECATED) != 0) {
            output.putShort(symbolTable.addConstantUtf8(Constants.DEPRECATED)).putInt(0);
        }
    }

    static final class Set {

        private static final int SIZE_INCREMENT = 6;

        private int size;

        private Attribute[] data = new Attribute[SIZE_INCREMENT];

        void addAttributes(final Attribute attributeList) {
            Attribute attribute = attributeList;
            while (attribute != null) {
                if (!contains(attribute)) {
                    add(attribute);
                }
                attribute = attribute.nextAttribute;
            }
        }

        Attribute[] toArray() {
            Attribute[] result = new Attribute[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        }

        private boolean contains(final Attribute attribute) {
            for (int i = 0; i < size; ++i) {
                if (data[i].type.equals(attribute.type)) {
                    return true;
                }
            }
            return false;
        }

        private void add(final Attribute attribute) {
            if (size >= data.length) {
                Attribute[] newData = new Attribute[data.length + SIZE_INCREMENT];
                System.arraycopy(data, 0, newData, 0, size);
                data = newData;
            }
            data[size++] = attribute;
        }

    }

}
